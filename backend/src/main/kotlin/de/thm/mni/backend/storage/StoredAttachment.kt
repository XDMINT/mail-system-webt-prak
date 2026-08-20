package de.thm.mni.backend.storage

data class StoredAttachment(
    val size: Long,
    val fileName: String,
    val mimeType: String?,
    val path: String,
)
