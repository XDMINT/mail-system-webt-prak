package de.thm.mni.backend.attachment.dto

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.attachment.AttachmentPolicy
import java.util.UUID
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Metadata for an attachment stored in the S3-compatible object store.")
data class AttachmentDTO(
    @field:Schema(description = "Attachment identifier.", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: UUID,
    @field:Schema(description = "Attachment size in bytes.", example = "42816", requiredMode = Schema.RequiredMode.REQUIRED)
    val size: Long,
    @field:Schema(
        description = "Original normalized file name.",
        example = "screenshot.png",
        minLength = 1,
        maxLength = AttachmentPolicy.MAX_FILE_NAME_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val fileName: String,
    @field:Schema(
        description = "Declared normalized media type, if valid.",
        example = "image/png",
        minLength = 1,
        maxLength = AttachmentPolicy.MAX_CONTENT_TYPE_LENGTH,
    )
    val mimeType: String?,
)

fun Attachment.toDTO() = AttachmentDTO(
    id = requireNotNull(this.id),
    fileName = this.fileName ?: "attachment",
    size = this.size,
    mimeType = this.mimeType,
)
