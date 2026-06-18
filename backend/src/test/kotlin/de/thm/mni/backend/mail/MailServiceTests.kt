package de.thm.mni.backend.mail

import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class MailServiceTests @Autowired constructor(
    private val mailService: MailService,
) {

    @Test
    fun `createImportedMail adds tracking code and deduplicates by external message id`() {
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
        assertNotNull(created.trackingCode)
        assertTrue(created.trackingCode!!.startsWith("TICKET-"))
        assertTrue(created.subject.startsWith("[${created.trackingCode}] "))
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
}
