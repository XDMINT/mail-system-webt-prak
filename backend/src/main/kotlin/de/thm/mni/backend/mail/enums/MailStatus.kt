package de.thm.mni.backend.mail.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Lifecycle status of a mail.")
enum class MailStatus {
    DRAFT,
    SENT,
    ERROR
}
