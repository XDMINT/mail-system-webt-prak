package de.thm.mni.backend.attachment

import de.thm.mni.backend.error.AttachmentTooLargeException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Component
class AttachmentPolicy(
    @Value("\${app.attachments.max-file-size}") maxFileSize: DataSize,
    @Value("\${app.attachments.max-total-size}") maxTotalSize: DataSize,
) {
    val maxFileSizeBytes: Long = maxFileSize.toBytes()
    val maxTotalSizeBytes: Long = maxTotalSize.toBytes()

    fun validateFileSize(size: Long) {
        if (size > maxFileSizeBytes) {
            throw AttachmentTooLargeException(
                "Attachment exceeds the maximum allowed size of $maxFileSizeBytes bytes"
            )
        }
    }

    fun validateTotalSize(size: Long) {
        if (size > maxTotalSizeBytes) {
            throw AttachmentTooLargeException(
                "Attachments exceed the maximum combined size of $maxTotalSizeBytes bytes"
            )
        }
    }

    fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            validateFileSize(total)
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    fun normalizeFileName(fileName: String?): String {
        val baseName = fileName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.replace(CONTROL_CHARACTERS, "")
            ?.trim()
            ?.take(MAX_FILE_NAME_LENGTH)
            .orEmpty()
        return baseName.ifBlank { DEFAULT_FILE_NAME }
    }

    fun normalizeContentType(contentType: String?): String? {
        val candidate = contentType
            ?.substringBefore(';')
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.takeIf { it.length <= MAX_CONTENT_TYPE_LENGTH }
        return candidate?.let { runCatching { MediaType.parseMediaType(it).toString() }.getOrNull() }
    }

    fun isPreviewable(contentType: String?): Boolean = normalizeContentType(contentType) in PREVIEWABLE_TYPES

    companion object {
        const val DEFAULT_FILE_NAME = "attachment"
        const val MAX_FILE_NAME_LENGTH = 255
        const val MAX_CONTENT_TYPE_LENGTH = 255
        private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]")
        private val PREVIEWABLE_TYPES = setOf(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp",
        )
    }
}
