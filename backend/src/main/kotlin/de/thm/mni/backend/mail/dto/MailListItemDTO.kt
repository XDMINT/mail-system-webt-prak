package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.user.dto.UserDTO
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Compact mail preview used by paged inbox listings.")
data class MailListItemDTO(
    @field:Schema(
        description = "Unique mail id.",
        example = "6f1f9368-d279-4a6e-993f-f0618767eeb8"
    )
    val id: UUID?,

    @field:Schema(description = "Sender of the mail.")
    val sender: UserDTO,

    @field:Schema(description = "Mail subject.", example = "[TICKET-3A9F1C7B] Support request")
    val subject: String,

    @field:Schema(description = "Body preview. The server truncates the content to 240 characters.", example = "Hello, I need help with my account.")
    val content: String,

    @field:Schema(description = "Current mail status.")
    val status: MailStatus,

    @field:Schema(description = "Whether the mail originated inside the app or was imported from IMAP.")
    val source: MailSource,

    @field:Schema(description = "Ticket tracking code.", example = "TICKET-3A9F1C7B")
    val trackingCode: String?,

    @field:Schema(description = "Original sender address for imported external mails.", example = "customer@example.com")
    val externalSenderEmail: String?,

    @field:Schema(description = "Number of attachments on this mail.", example = "2")
    val attachmentCount: Int,

    @field:Schema(description = "Creation timestamp.", example = "2026-06-25T12:30:00")
    val createdAt: LocalDateTime,

    @field:Schema(description = "Last update timestamp.", example = "2026-06-25T12:45:00")
    val updatedAt: LocalDateTime,

    @field:Schema(description = "Timestamp at which the mail was sent or imported.", example = "2026-06-25T13:00:00")
    val sentAt: LocalDateTime?,
)
