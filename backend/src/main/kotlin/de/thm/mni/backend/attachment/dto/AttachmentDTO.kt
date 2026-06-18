package de.thm.mni.backend.attachment.dto

import de.thm.mni.backend.attachment.Attachment
import java.util.UUID

data class AttachmentDTO(
    val id: UUID?,
    val size: Long,
    val fileName: String?,
    val mimeType: String?,
    val path: String,
)

fun Attachment.toDTO() = AttachmentDTO(
    id = this.id,
    fileName = this.fileName,
    size = this.size,
    mimeType = this.mimeType,
    path = this.path,
)
