package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.attachment.dto.AttachmentDTO
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.user.dto.UserDTO
import java.time.LocalDateTime
import java.util.UUID
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Complete mail representation including recipients and attachments.")
data class MailDTO(
    @field:Schema(description = "Mail identifier.", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: UUID,
    @field:Schema(description = "Internal author or external sender contact.", requiredMode = Schema.RequiredMode.REQUIRED)
    val sender: UserDTO,
    @field:Schema(description = "Mail subject.", example = "[TICKET-1A2B3C4D] Re: Registration", requiredMode = Schema.RequiredMode.REQUIRED)
    val subject: String,
    @field:Schema(description = "Plain-text mail body.", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String,
    @field:Schema(description = "Current delivery state.", requiredMode = Schema.RequiredMode.REQUIRED)
    val status: MailStatus,
    @field:Schema(description = "Whether the mail originated inside the application or from IMAP.", requiredMode = Schema.RequiredMode.REQUIRED)
    val source: MailSource,
    @field:Schema(description = "Ticket number assigned to the support conversation.", example = "TICKET-1A2B3C4D")
    val trackingCode: String?,
    @field:Schema(description = "Original sender address for an imported support mail.", example = "customer@example.org")
    val externalSenderEmail: String?,
    @field:Schema(description = "Identifier of the incoming mail answered by this draft.")
    val inReplyToMailId: UUID?,
    @field:Schema(description = "Direct recipients.", requiredMode = Schema.RequiredMode.REQUIRED)
    val to: List<UserDTO>,
    @field:Schema(description = "Carbon-copy recipients.", requiredMode = Schema.RequiredMode.REQUIRED)
    val cc: List<UserDTO>,
    @field:Schema(description = "Blind-carbon-copy recipients.", requiredMode = Schema.RequiredMode.REQUIRED)
    val bcc: List<UserDTO>,
    @field:Schema(description = "Stored attachments.", requiredMode = Schema.RequiredMode.REQUIRED)
    val attachments: List<AttachmentDTO>,
    @field:Schema(description = "Creation timestamp.", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: LocalDateTime,
    @field:Schema(description = "Last modification timestamp.", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: LocalDateTime,
    @field:Schema(description = "Successful delivery or external receipt timestamp.")
    val sentAt: LocalDateTime?,
)
