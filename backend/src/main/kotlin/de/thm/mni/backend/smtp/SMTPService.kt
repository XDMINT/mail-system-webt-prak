package de.thm.mni.backend.smtp

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.storage.FileStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets


@Service
class SMTPService(
    private val javaMailSender: JavaMailSender,
    private val mailRecordService: MailRecordService,
    private val fileStorageService: FileStorageService,
    @Value("\${mail.from-address:\${spring.mail.username:}}") private val fromAddress: String,
) {
    fun sendEmail(mail: Mail): Boolean {
        return try {
            val recipients = mail.id?.let { mailRecordService.getMailRecordByMailId(it) }.orEmpty()
            if (recipients.isEmpty()) {
                return true
            }

            val message = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, StandardCharsets.UTF_8.name())
            helper.setFrom(fromAddress.ifBlank { mail.sender?.email ?: "no-reply@thm.local" })

            val toRecipients = recipients.filter { it.type == MailType.TO }.mapNotNull { it.user?.email }.distinct()
            val ccRecipients = recipients.filter { it.type == MailType.CC }.mapNotNull { it.user?.email }.distinct()
            val bccRecipients = recipients.filter { it.type == MailType.BCC }.mapNotNull { it.user?.email }.distinct()

            if (toRecipients.isNotEmpty()) helper.setTo(toRecipients.toTypedArray())
            if (ccRecipients.isNotEmpty()) helper.setCc(ccRecipients.toTypedArray())
            if (bccRecipients.isNotEmpty()) helper.setBcc(bccRecipients.toTypedArray())

            helper.setSubject(mail.subject)
            helper.setText(mail.content, false)

            mail.attachments.forEach { attachment ->
                helper.addAttachment(attachment.fileName ?: attachment.path, fileStorageService.load(attachment.path))
            }

            javaMailSender.send(message)
            true
        } catch (_: Exception) {
            false
        }
    }
}