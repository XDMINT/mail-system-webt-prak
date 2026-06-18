package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.user.dto.UserDTO
import java.time.LocalDateTime
import java.util.UUID

data class MailListItemDTO(
    val id: UUID?,
    val sender: UserDTO,
    val subject: String,
    val content: String,
    val status: MailStatus,
    val source: MailSource,
    val trackingCode: String?,
    val externalSenderEmail: String?,
    val attachmentCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val sentAt: LocalDateTime?,
)
