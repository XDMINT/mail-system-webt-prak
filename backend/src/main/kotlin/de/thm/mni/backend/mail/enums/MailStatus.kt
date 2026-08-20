package de.thm.mni.backend.mail.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Mail lifecycle and SMTP delivery status.")
enum class MailStatus {
    DRAFT,
    SENT,
    ERROR
}
