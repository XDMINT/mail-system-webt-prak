package de.thm.mni.backend.mail

import de.thm.mni.backend.mail.dto.ImportedAttachment
import de.thm.mni.backend.mail.dto.MailCreate
import de.thm.mni.backend.mail.dto.MailUpdate
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.smtp.OutboundMailGateway
import de.thm.mni.backend.user.UserService
import de.thm.mni.backend.user.User
import de.thm.mni.backend.storage.FileStorageService
import de.thm.mni.backend.storage.StoredAttachment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import org.junit.jupiter.api.Assertions.assertThrows
import de.thm.mni.backend.error.ResourceCannotBeModifiedException

@SpringBootTest
@Transactional
class MailServiceTests @Autowired constructor(
    private val mailService: MailService,
    private val userService: UserService,
    private val mailRecordService: MailRecordService,
) {

    @MockitoBean
    private lateinit var outboundMailGateway: OutboundMailGateway

    @MockitoBean
    private lateinit var fileStorageService: FileStorageService

    @Test
    fun `createImportedMail preserves a new subject without assigning a ticket and deduplicates`() {
        val externalMessageId = "test-message-${System.nanoTime()}@example.com"

        val created = mailService.createImportedMail(
            senderEmail = "sender-${System.nanoTime()}@example.com",
            subject = "Support request",
            content = "Please help",
            attachments = emptyList(),
            externalMessageId = externalMessageId,
            receivedAt = null,
        )
        val duplicate = mailService.createImportedMail(
            senderEmail = "other-sender@example.com",
            subject = "Different subject",
            content = "Different content",
            attachments = emptyList(),
            externalMessageId = externalMessageId,
            receivedAt = null,
        )

        assertEquals(created.id, duplicate.id)
        assertEquals(MailSource.EXTERN, created.source)
        assertEquals(MailStatus.SENT, created.status)
        assertNull(created.trackingCode)
        assertEquals("Support request", created.subject)
    }

    @Test
    fun `seed data contains a shared external support request without a ticket`() {
        val supportRequest = mailService.getMailByExternalMessageId("demo-support-request@example.org")!!

        assertEquals(MailSource.EXTERN, supportRequest.source)
        assertEquals("erika.external@example.org", supportRequest.externalSenderEmail)
        assertNull(supportRequest.trackingCode)
        assertEquals(5, mailRecordService.getMailRecordByMailId(supportRequest.id!!).size)
    }

    @Test
    fun `a newly provisioned internal user can access older external support mails`() {
        val newUser = userService.createUser(
            User("New", "Support Agent", "new-agent-${System.nanoTime()}@example.com")
        )
        val supportRequest = mailService.getMailByExternalMessageId("demo-support-request@example.org")!!

        val incoming = mailRecordService.getIncomingMailsForUser(newUser.id!!, PageRequest.of(0, 100))

        assertTrue(incoming.content.any { it.id == supportRequest.id })
    }

    @Test
    fun `an incoming mail with multiple recipient records occurs only once`() {
        val internalUser = userService.getUserByEmail("aallanson@example.com")!!
        val supportRequest = mailService.getMailByExternalMessageId("demo-support-request@example.org")!!

        val incoming = mailRecordService.getIncomingMailsForUser(internalUser.id!!, PageRequest.of(0, 100))

        assertEquals(1, incoming.content.count { it.id == supportRequest.id })
    }

    @Test
    fun `createImportedMail allows multiple mails with same ticket code`() {
        val ticketCode = "TICKET-1A2B3C4D"
        val first = mailService.createImportedMail(
            senderEmail = "sender-one-${System.nanoTime()}@example.com",
            subject = "[$ticketCode] Support request",
            content = "First message",
            attachments = emptyList(),
            externalMessageId = "first-${System.nanoTime()}@example.com",
            receivedAt = null,
        )
        val second = mailService.createImportedMail(
            senderEmail = "sender-two-${System.nanoTime()}@example.com",
            subject = "[$ticketCode] Re: Support request",
            content = "Second message",
            attachments = emptyList(),
            externalMessageId = "second-${System.nanoTime()}@example.com",
            receivedAt = null,
        )

        assertEquals(ticketCode, first.trackingCode)
        assertEquals(ticketCode, second.trackingCode)
    }

    @Test
    fun `createReplyDraft assigns one ticket and addresses the external sender`() {
        val incoming = mailService.createImportedMail(
            senderEmail = "reply-target-${System.nanoTime()}@example.com",
            subject = "Need help",
            content = "Please help",
            attachments = emptyList(),
            externalMessageId = "reply-source-${System.nanoTime()}@example.com",
            receivedAt = null,
        )
        val internalUser = userService.getUserByEmail("aallanson@example.com")!!

        val firstReply = mailService.createReplyDraft(incoming, internalUser)
        val secondReply = mailService.createReplyDraft(incoming, internalUser)

        assertTrue(firstReply.trackingCode!!.startsWith("TICKET-"))
        assertEquals(firstReply.trackingCode, secondReply.trackingCode)
        assertEquals(incoming.id, firstReply.inReplyToMail?.id)
        assertEquals("[${firstReply.trackingCode}] Re: Need help", firstReply.subject)
        assertEquals("Need help", incoming.subject)
        val recipients = mailRecordService.getMailRecordByMailId(firstReply.id!!)
        assertEquals(1, recipients.size)
        assertEquals(MailType.TO, recipients.single().type)
        assertEquals(incoming.externalSenderEmail, recipients.single().user?.email)
    }

    @Test
    fun `sendMail persists an error when SMTP delivery fails`() {
        val incoming = mailService.createImportedMail(
            senderEmail = "delivery-target-${System.nanoTime()}@example.com",
            subject = "Delivery test",
            content = "Please reply",
            attachments = emptyList(),
            externalMessageId = "delivery-source-${System.nanoTime()}@example.com",
            receivedAt = null,
        )
        val internalUser = userService.getUserByEmail("aallanson@example.com")!!
        val reply = mailService.createReplyDraft(incoming, internalUser)
        Mockito.`when`(outboundMailGateway.send(reply)).thenReturn(false)

        val result = mailService.sendMail(reply)

        assertEquals(MailStatus.ERROR, result.status)
        assertEquals(MailStatus.ERROR, mailService.getMailById(reply.id!!)!!.status)
    }

    @Test
    fun `stored IMAP attachments are deleted when the database transaction rolls back`() {
        val content = byteArrayOf(1, 2, 3, 4)
        val storedAttachment = StoredAttachment(
            size = 4,
            fileName = "test.txt",
            mimeType = "text/plain",
            path = "attachments/test.txt",
        )
        Mockito.`when`(fileStorageService.saveFile("test.txt", "text/plain", content))
            .thenReturn(storedAttachment)

        mailService.createImportedMail(
            senderEmail = "rollback-${System.nanoTime()}@example.com",
            subject = "Rollback attachment",
            content = "Test",
            attachments = listOf(ImportedAttachment("test.txt", "text/plain", content)),
            externalMessageId = "rollback-${System.nanoTime()}@example.com",
            receivedAt = null,
        )

        TestTransaction.flagForRollback()
        TestTransaction.end()

        Mockito.verify(fileStorageService).deleteFile("attachments/test.txt")
    }

    @Test
    fun `updating a draft retains selected attachments without downloading or uploading them again`() {
        val sender = userService.getUserByEmail("aallanson@example.com")!!
        val receiver = userService.getUserByEmail("svardey1@example.com")!!
        val file = MockMultipartFile("attachments", "note.txt", "text/plain", "hello".toByteArray())
        Mockito.`when`(fileStorageService.saveFile(file)).thenReturn(
            StoredAttachment(5, "note.txt", "text/plain", "attachments/note")
        )
        val draft = mailService.createMail(
            MailCreate("Subject", "Content", mutableListOf(receiver.id!!), mutableListOf(), mutableListOf()),
            sender,
            listOf(file),
        )
        val attachmentId = draft.attachments.single().id!!

        val updated = mailService.updateMail(
            draft.id!!,
            MailUpdate(
                "Updated",
                "Updated content",
                mutableListOf(receiver.id!!),
                mutableListOf(),
                mutableListOf(),
                listOf(attachmentId),
            ),
            emptyList(),
        )

        assertEquals(listOf(attachmentId), updated.attachments.map { it.id })
        Mockito.verify(fileStorageService, Mockito.times(1)).saveFile(file)
    }

    @Test
    fun `updating a draft rejects attachment identifiers from another mail`() {
        val sender = userService.getUserByEmail("aallanson@example.com")!!
        val receiver = userService.getUserByEmail("svardey1@example.com")!!
        val draft = mailService.createMail(
            MailCreate("Subject", "Content", mutableListOf(receiver.id!!), mutableListOf(), mutableListOf()),
            sender,
            emptyList(),
        )

        assertThrows(ResourceCannotBeModifiedException::class.java) {
            mailService.updateMail(
                draft.id!!,
                MailUpdate(
                    "Updated",
                    "Updated content",
                    mutableListOf(receiver.id!!),
                    mutableListOf(),
                    mutableListOf(),
                    listOf(java.util.UUID.randomUUID()),
                ),
                emptyList(),
            )
        }
    }
}
