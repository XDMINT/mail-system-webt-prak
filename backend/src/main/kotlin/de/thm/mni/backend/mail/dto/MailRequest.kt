package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.validation.AtLeastOneRecipient
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.util.UUID


@AtLeastOneRecipient
@Schema(description = "Mail payload used inside the multipart `data` part for creating, sending, and updating mails.")
data class MailRequest(
    @field:Schema(
        description = "Mail subject. Replies may receive a ticket prefix when sent.",
        example = "Support request"
    )
    @field:Size(min = 1, max = 20, message = "Subject must be between 1 and 20 characters")
    val subject: String,

    @field:Schema(
        description = "Plain text mail body.",
        example = "Hello, I need help with my account."
    )
    @field:Size(min = 1, max = 500, message = "Content must be between 1 and 500 characters")
    val content: String,

    @field:Schema(description = "Direct recipient user ids.")
    val toIds: MutableList<UUID>,

    @field:Schema(description = "Carbon-copy recipient user ids.")
    val ccIds: MutableList<UUID>,

    @field:Schema(description = "Blind-copy recipient user ids.")
    val bccIds: MutableList<UUID>,

    @field:Schema(description = "User ids this mail replies to.")
    val replyToIds: MutableList<UUID>
)


fun MailRequest.toMailCreate(): MailCreate {
    return MailCreate(
        subject = this.subject,
        content = this.content,
        toIds = this.toIds,
        ccIds = this.ccIds,
        bccIds = this.bccIds,
        replyToIds = this.replyToIds
    )
}

fun MailRequest.toMailUpdate(): MailUpdate {
    return MailUpdate(
        subject = this.subject,
        content = this.content,
        toIds = this.toIds,
        ccIds = this.ccIds,
        bccIds = this.bccIds,
        replyToIds = this.replyToIds
    )
}
