package de.thm.mni.backend.mail.dto

data class ImportedAttachment(
    val fileName: String,
    val contentType: String?,
    val bytes: ByteArray,
)

