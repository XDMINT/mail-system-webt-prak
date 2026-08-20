package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.AttachmentRepository
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.user.CurrentUserService
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.util.UUID


@RestController
@RequestMapping("/api/attachments")
class StorageController(
    private val fileStorageService: FileStorageService,
    private val attachmentRepository: AttachmentRepository,
    private val mailRecordService: MailRecordService,
    private val currentUserService: CurrentUserService,
) {
    @GetMapping("/{attachmentId}")
    fun getAttachment(
        @PathVariable attachmentId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Resource> {
        val attachment = attachmentRepository.findById(attachmentId).orElse(null)
            ?: throw ResourceNotFoundException("Attachment not found")
        val mail = attachment.mail ?: throw ResourceNotFoundException("Attachment not found")
        val userId = currentUserService.getOrProvision(jwt).id!!
        val records = mail.id?.let { mailRecordService.getMailRecordByMailId(it) }.orEmpty()
        val canAccess = mail.source == MailSource.EXTERN || mail.sender?.id == userId || records.any { it.user?.id == userId }

        if (!canAccess) {
            throw ResourceNotFoundException("Attachment not found")
        }

        val storedFile = fileStorageService.load(attachment.path)
        val contentType = attachment.mimeType ?: storedFile.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .contentLength(storedFile.contentLength ?: attachment.size)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline()
                    .filename(attachment.fileName ?: attachment.path, StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .body(storedFile.resource)
    }
}
