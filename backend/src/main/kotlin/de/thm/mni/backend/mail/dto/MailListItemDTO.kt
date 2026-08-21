package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.user.dto.UserDTO
import java.time.LocalDateTime
import java.util.UUID
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Compact mail representation used in paginated inbox results.")
data class MailListItemDTO(
    @field:Schema(description = "Mail identifier.", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: UUID,
    @field:Schema(description = "Internal author or external sender contact.", requiredMode = Schema.RequiredMode.REQUIRED)
    val sender: UserDTO,
    @field:Schema(description = "Mail subject.", requiredMode = Schema.RequiredMode.REQUIRED)
    val subject: String,
    @field:Schema(description = "Plain-text mail body.", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String,
    @field:Schema(description = "Current delivery state.", requiredMode = Schema.RequiredMode.REQUIRED)
    val status: MailStatus,
    @field:Schema(description = "Mail origin.", requiredMode = Schema.RequiredMode.REQUIRED)
    val source: MailSource,
    @field:Schema(description = "Ticket number, if one is known.")
    val trackingCode: String?,
    @field:Schema(description = "Original external sender address.")
    val externalSenderEmail: String?,
    @field:Schema(description = "Number of stored attachments.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    val attachmentCount: Int,
    @field:Schema(description = "Creation timestamp.", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: LocalDateTime,
    @field:Schema(description = "Last modification timestamp.", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: LocalDateTime,
    @field:Schema(description = "Successful delivery or external receipt timestamp.")
    val sentAt: LocalDateTime?,
)
