package de.thm.mni.backend.mail.imap

import de.thm.mni.backend.mail.MailService
import jakarta.mail.Flags
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.util.Properties

class MailInboxSyncServiceTests {
    private val mailService = Mockito.mock(MailService::class.java)
    private val syncService = MailInboxSyncService(
        mailService = mailService,
        host = "",
        port = 993,
        username = "",
        password = "",
        folderName = "INBOX",
    )

    @Test
    fun `successful import marks the message as seen`() {
        val message = message("successful-import@example.org")

        val result = syncService.importUnseenMessages(arrayOf(message))

        assertEquals(1, result.imported)
        assertEquals(0, result.failed)
        assertTrue(message.isSet(Flags.Flag.SEEN))
    }

    @Test
    fun `failed import keeps the message unseen for a retry`() {
        val message = message("failed-import@example.org")
        Mockito.`when`(
            mailService.createImportedMail(
                senderEmail = ArgumentMatchers.anyString(),
                subject = ArgumentMatchers.anyString(),
                content = ArgumentMatchers.anyString(),
                attachments = ArgumentMatchers.anyList(),
                externalMessageId = ArgumentMatchers.anyString(),
                receivedAt = ArgumentMatchers.any(),
            )
        ).thenThrow(IllegalStateException("storage unavailable"))

        val result = syncService.importUnseenMessages(arrayOf(message))

        assertEquals(0, result.imported)
        assertEquals(1, result.failed)
        assertFalse(message.isSet(Flags.Flag.SEEN))
    }

    private fun message(messageId: String): MimeMessage = MimeMessage(Session.getInstance(Properties())).apply {
        setFrom(InternetAddress("customer@example.org"))
        setRecipients(jakarta.mail.Message.RecipientType.TO, "support@thm.de")
        subject = "Support request"
        setText("Please help")
        saveChanges()
        setHeader("Message-ID", "<$messageId>")
    }
}
