package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.AttachmentPolicy
import de.thm.mni.backend.attachment.AttachmentRepository
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail_record.MailRecordService
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AttachmentDownloadService(
    private val attachmentRepository: AttachmentRepository,
    private val fileStorageService: FileStorageService,
    private val mailRecordService: MailRecordService,
    private val attachmentPolicy: AttachmentPolicy,
) {
    fun load(attachmentId: UUID, userId: UUID, preview: Boolean): AttachmentDownload {
        val attachment = attachmentRepository.findById(attachmentId).orElse(null)
            ?: throw ResourceNotFoundException("Attachment not found")
        val mail = attachment.mail ?: throw ResourceNotFoundException("Attachment not found")
        val records = mail.id?.let(mailRecordService::getMailRecordByMailId).orEmpty()
        val canAccess = mail.source == MailSource.EXTERN ||
            mail.sender?.id == userId ||
            records.any { it.user?.id == userId }

        if (!canAccess) {
            throw ResourceNotFoundException("Attachment not found")
        }

        val storedFile = fileStorageService.load(attachment.path)
        val inline = preview && attachmentPolicy.isPreviewable(attachment.mimeType)
        val contentType = if (inline) {
            MediaType.parseMediaType(requireNotNull(attachmentPolicy.normalizeContentType(attachment.mimeType)))
        } else {
            MediaType.APPLICATION_OCTET_STREAM
        }

        return AttachmentDownload(
            resource = storedFile.resource,
            contentType = contentType,
            contentLength = storedFile.contentLength ?: attachment.size,
            fileName = attachmentPolicy.normalizeFileName(attachment.fileName),
            inline = inline,
        )
    }
}

data class AttachmentDownload(
    val resource: Resource,
    val contentType: MediaType,
    val contentLength: Long,
    val fileName: String,
    val inline: Boolean,
)
