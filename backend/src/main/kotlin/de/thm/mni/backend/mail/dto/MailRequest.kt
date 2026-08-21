package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.validation.AtLeastOneRecipient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID
import io.swagger.v3.oas.annotations.media.Schema


@AtLeastOneRecipient
@Schema(description = "Mail content and recipients used to create a draft or send a new mail.")
data class MailRequest(
    @field:Schema(description = "Mail subject.", example = "Question about registration", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "Subject must not be blank")
    @field:Size(min = 1, max = 255, message = "Subject must be between 1 and 255 characters")
    override val subject: String,
    @field:Schema(description = "Plain-text mail body.", example = "How can we help you?", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "Content must not be blank")
    @field:Size(min = 1, max = 500, message = "Content must be between 1 and 500 characters")
    override val content: String,
    @field:Schema(description = "Identifiers of direct recipients.", requiredMode = Schema.RequiredMode.REQUIRED)
    override val toIds: MutableList<UUID>,
    @field:Schema(description = "Identifiers of carbon-copy recipients.", requiredMode = Schema.RequiredMode.REQUIRED)
    override val ccIds: MutableList<UUID>,
    @field:Schema(description = "Identifiers of blind-carbon-copy recipients.", requiredMode = Schema.RequiredMode.REQUIRED)
    override val bccIds: MutableList<UUID>,
) : RecipientMailRequest


fun MailRequest.toMailCreate(): MailCreate {
    return MailCreate(
        subject = this.subject,
        content = this.content,
        toIds = this.toIds,
        ccIds = this.ccIds,
        bccIds = this.bccIds,
    )
}
