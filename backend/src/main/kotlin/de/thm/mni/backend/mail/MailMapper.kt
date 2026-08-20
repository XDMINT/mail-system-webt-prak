package de.thm.mni.backend.mail

import de.thm.mni.backend.attachment.dto.toDTO
import de.thm.mni.backend.mail.dto.MailDTO
import de.thm.mni.backend.mail.dto.MailListItemDTO
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.MailRecord
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.dto.toDTO
import org.springframework.stereotype.Component


@Component
class MailMapper(private val mailRecordService: MailRecordService) {
    fun toListItemDTO(mail: Mail): MailListItemDTO {
        return MailListItemDTO(
            id = requireNotNull(mail.id) { "Cannot map a mail without an identifier" },
            sender = mail.sender?.toDTO()!!,
            subject = mail.subject,
            content = mail.content.take(PREVIEW_LENGTH),
            status = mail.status,
            source = mail.source,
            trackingCode = mail.trackingCode,
            externalSenderEmail = mail.externalSenderEmail,
            attachmentCount = mail.attachments.size,
            createdAt = mail.createdAt,
            updatedAt = mail.updatedAt,
            sentAt = mail.sentAt
        )
    }

    fun toDTO(user: User, mail: Mail): MailDTO {
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)
        return MailDTO(
            id = requireNotNull(mail.id) { "Cannot map a mail without an identifier" },
            sender = mail.sender?.toDTO()!!,
            subject = mail.subject,
            content = mail.content,
            status = mail.status,
            source = mail.source,
            trackingCode = mail.trackingCode,
            externalSenderEmail = mail.externalSenderEmail,
            inReplyToMailId = mail.inReplyToMail?.id,
            to = records.filter { it.type == MailType.TO }.map { it.user!!.toDTO() },
            cc = records.filter { it.type == MailType.CC }.map { it.user!!.toDTO() },
            bcc = records.filter { it.type == MailType.BCC && (it.user!!.id == user.id || mail.sender!!.id == user.id) }.map { it.user!!.toDTO() },
            attachments = mail.attachments.map { it -> it.toDTO() },
            createdAt = mail.createdAt,
            updatedAt = mail.updatedAt,
            sentAt = mail.sentAt
        )

    }

    private companion object {
        private const val PREVIEW_LENGTH = 240
    }
}
