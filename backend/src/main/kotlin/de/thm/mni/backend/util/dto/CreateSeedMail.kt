package de.thm.mni.backend.util.dto

import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailSource

data class CreateSeedMail(
    val senderEmail: String,
    val subject: String,
    val content: String,
    val status: MailStatus,
    val toEmails: List<String>,
    val ccEmails: List<String>,
    val bccEmails: List<String>,
    val source: MailSource = MailSource.INTERN,
    val externalMessageId: String? = null,
    val externalSenderEmail: String? = null,
)
