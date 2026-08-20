package de.thm.mni.backend.mail.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Origin of a mail: created in the application or imported from IMAP.")
enum class MailSource {
    EXTERN,
    INTERN
}
