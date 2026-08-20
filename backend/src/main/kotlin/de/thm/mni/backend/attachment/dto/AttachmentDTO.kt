package de.thm.mni.backend.attachment.dto

import de.thm.mni.backend.attachment.Attachment
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Metadata for a file attached to a mail.")
data class AttachmentDTO(
    @field:Schema(
        description = "Unique attachment id used for downloads.",
        example = "3f75cd3b-3caa-4765-bdc3-c40279a1975a"
    )
    val id: UUID?,

    @field:Schema(description = "Attachment size in bytes.", example = "18452")
    val size: Long,

    @field:Schema(description = "Original file name.", example = "invoice.pdf")
    val fileName: String?,

    @field:Schema(description = "Detected media type.", example = "application/pdf")
    val mimeType: String?,

    @field:Schema(
        description = "Internal storage key. Use the attachment id with `GET /api/attachments/{attachmentId}` instead of addressing this path directly.",
        example = "attachments/3f75cd3b-3caa-4765-bdc3-c40279a1975a.pdf"
    )
    val path: String,
)

fun Attachment.toDTO() = AttachmentDTO(
    id = this.id,
    fileName = this.fileName,
    size = this.size,
    mimeType = this.mimeType,
    path = this.path,
)
