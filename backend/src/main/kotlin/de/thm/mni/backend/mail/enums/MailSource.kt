package de.thm.mni.backend.mail.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Origin of a mail.")
enum class MailSource {
    EXTERN,
    INTERN
}
