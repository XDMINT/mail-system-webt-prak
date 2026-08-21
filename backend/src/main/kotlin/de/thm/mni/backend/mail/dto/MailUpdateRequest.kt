package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.validation.AtLeastOneRecipient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID
import io.swagger.v3.oas.annotations.media.Schema

@AtLeastOneRecipient
@Schema(description = "Mail data used to update a draft while retaining selected stored attachments.")
data class MailUpdateRequest(
    @field:Schema(description = "Mail subject.", example = "[TICKET-1A2B3C4D] Re: Registration", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "Subject must not be blank")
    @field:Size(min = 1, max = 255, message = "Subject must be between 1 and 255 characters")
    override val subject: String,
    @field:Schema(description = "Plain-text mail body.", example = "Thank you for contacting us.", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "Content must not be blank")
    @field:Size(min = 1, max = 500, message = "Content must be between 1 and 500 characters")
    override val content: String,
    @field:Schema(description = "Identifiers of direct recipients.", requiredMode = Schema.RequiredMode.REQUIRED)
    override val toIds: MutableList<UUID>,
    @field:Schema(description = "Identifiers of carbon-copy recipients.", requiredMode = Schema.RequiredMode.REQUIRED)
    override val ccIds: MutableList<UUID>,
    @field:Schema(description = "Identifiers of blind-carbon-copy recipients.", requiredMode = Schema.RequiredMode.REQUIRED)
    override val bccIds: MutableList<UUID>,
    @field:Schema(description = "Existing attachment identifiers that must remain associated with the draft.")
    val retainedAttachmentIds: List<UUID> = emptyList(),
) : RecipientMailRequest

fun MailUpdateRequest.toMailUpdate() = MailUpdate(
    subject = subject,
    content = content,
    toIds = toIds,
    ccIds = ccIds,
    bccIds = bccIds,
    retainedAttachmentIds = retainedAttachmentIds,
)
