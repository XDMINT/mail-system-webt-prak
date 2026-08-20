package de.thm.mni.backend.mail.dto

import java.util.UUID

interface RecipientMailRequest {
    val subject: String
    val content: String
    val toIds: MutableList<UUID>
    val ccIds: MutableList<UUID>
    val bccIds: MutableList<UUID>
}
