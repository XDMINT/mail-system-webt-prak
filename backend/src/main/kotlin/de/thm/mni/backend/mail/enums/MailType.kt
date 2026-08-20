package de.thm.mni.backend.mail.enums

enum class MailType {
    TO,
    CC,
    BCC,
    @Deprecated("Legacy compatibility only; replies are linked through Mail.inReplyToMail")
    REPLY_TO
}
