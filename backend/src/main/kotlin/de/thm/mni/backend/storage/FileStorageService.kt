package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.AttachmentPolicy
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile


@Service
class FileStorageService(
    private val fileStorageRepository: FileStorageRepository,
    private val attachmentPolicy: AttachmentPolicy,
) {

    fun saveFile(file: MultipartFile): StoredAttachment? {
        if (file.isEmpty) return null
        attachmentPolicy.validateFileSize(file.size)
        return saveFile(file.originalFilename ?: "attachment", file.contentType, file.bytes)
    }

    fun saveFile(fileName: String, contentType: String?, content: ByteArray): StoredAttachment {
        attachmentPolicy.validateFileSize(content.size.toLong())
        return fileStorageRepository.saveFile(
            attachmentPolicy.normalizeFileName(fileName),
            attachmentPolicy.normalizeContentType(contentType),
            content,
        )
    }

    fun deleteFile(filename: String?) {
        requireNotNull(filename) { "Filename must not be null" }
        return fileStorageRepository.deleteFile(filename)
    }

    fun load(filename: String): StoredFile {
        return fileStorageRepository.load(filename)
    }
}
