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
import org.slf4j.LoggerFactory


@Service
class SMTPService(
    private val javaMailSender: JavaMailSender,
    private val mailRecordService: MailRecordService,
    private val fileStorageService: FileStorageService,
    @Value("\${mail.from-address:\${spring.mail.username:}}") private val fromAddress: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendEmail(mail: Mail): Boolean {
        return try {
            if (fromAddress.isBlank()) {
                logger.warn("SMTP send aborted: mail.from-address is empty")
                return false
            }

            val recipients = mail.id?.let { mailRecordService.getMailRecordByMailId(it) }.orEmpty()
            if (recipients.isEmpty()) {
                return true
            }

            val message = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, StandardCharsets.UTF_8.name())
            helper.setFrom(fromAddress)

            val toRecipients = recipients.filter { it.type == MailType.TO }.mapNotNull { it.user?.email }.distinct()
            val ccRecipients = recipients.filter { it.type == MailType.CC }.mapNotNull { it.user?.email }.distinct()
            val bccRecipients = recipients.filter { it.type == MailType.BCC }.mapNotNull { it.user?.email }.distinct()

            if (toRecipients.isNotEmpty()) helper.setTo(toRecipients.toTypedArray())
            if (ccRecipients.isNotEmpty()) helper.setCc(ccRecipients.toTypedArray())
            if (bccRecipients.isNotEmpty()) helper.setBcc(bccRecipients.toTypedArray())

            helper.setSubject(mail.subject)
            helper.setText(mail.content, false)

            mail.attachments.forEach { attachment ->
                helper.addAttachment(
                    attachment.fileName ?: attachment.path,
                    fileStorageService.load(attachment.path).resource
                )
            }

            javaMailSender.send(message)
            true
        } catch (ex: Exception) {
            logger.warn("SMTP send failed for mail {}", mail.id, ex)
            false
        }
    }
}
