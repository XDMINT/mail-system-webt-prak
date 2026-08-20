package de.thm.mni.backend.smtp

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.MailRecord
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.storage.FileStorageService
import de.thm.mni.backend.user.User
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mail.javamail.JavaMailSender
import java.util.Properties
import java.util.UUID

class SMTPServiceTests {
    private val javaMailSender = Mockito.mock(JavaMailSender::class.java)
    private val mailRecordService = Mockito.mock(MailRecordService::class.java)
    private val fileStorageService = Mockito.mock(FileStorageService::class.java)

    @Test
    fun `send fails when no recipient is configured`() {
        val mail = Mail().also { it.id = UUID.randomUUID() }
        Mockito.`when`(mailRecordService.getMailRecordByMailId(mail.id!!)).thenReturn(emptyList())
        val gateway = SMTPService(javaMailSender, mailRecordService, fileStorageService, "support@thm.de")

        assertFalse(gateway.send(mail))
        Mockito.verifyNoInteractions(javaMailSender)
    }

    @Test
    fun `send uses configured support address and external sender as recipient`() {
        val mail = Mail().also {
            it.id = UUID.randomUUID()
            it.subject = "Reply"
            it.content = "Answer"
        }
        val externalUser = User("External", "Sender", "customer@example.org", externalContact = true).also {
            it.id = UUID.randomUUID()
        }
        val record = MailRecord(mail, externalUser, MailType.TO)
        val message = MimeMessage(Session.getInstance(Properties()))
        Mockito.`when`(mailRecordService.getMailRecordByMailId(mail.id!!)).thenReturn(listOf(record))
        Mockito.`when`(javaMailSender.createMimeMessage()).thenReturn(message)
        val gateway = SMTPService(javaMailSender, mailRecordService, fileStorageService, "support@thm.de")

        assertTrue(gateway.send(mail))
        assertEquals("support@thm.de", (message.from.single() as InternetAddress).address)
        assertEquals("customer@example.org", (message.getRecipients(Message.RecipientType.TO).single() as InternetAddress).address)
        Mockito.verify(javaMailSender).send(message)
    }
}
