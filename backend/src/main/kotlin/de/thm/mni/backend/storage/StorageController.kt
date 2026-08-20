package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.AttachmentRepository
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.openapi.DefaultApiErrors
import de.thm.mni.backend.openapi.NotFoundApiError
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.util.UUID


@RestController
@RequestMapping("/api/attachments")
@Tag(
    name = "Attachments",
    description = "Download mail attachments after sender or recipient access checks."
)
@SecurityRequirement(name = "bearerAuth")
@DefaultApiErrors
class StorageController(
    private val fileStorageService: FileStorageService,
    private val attachmentRepository: AttachmentRepository,
    private val mailRecordService: MailRecordService,
) {
    @GetMapping("/{attachmentId}")
    @Operation(
        operationId = "downloadAttachment",
        summary = "Download an attachment",
        description = "Streams an attachment if the authenticated user is the sender or recipient of the related mail. Unauthorized access is hidden as 404."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Attachment file stream returned successfully. The response content type is based on the stored attachment metadata."
    )
    @NotFoundApiError
    fun getAttachment(
        @Parameter(description = "Attachment id.", example = "3f75cd3b-3caa-4765-bdc3-c40279a1975a")
        @PathVariable attachmentId: UUID,
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails,
    ): ResponseEntity<Resource> {
        val attachment = attachmentRepository.findById(attachmentId).orElse(null)
            ?: throw ResourceNotFoundException("Attachment not found")
        val mail = attachment.mail ?: throw ResourceNotFoundException("Attachment not found")
        val userId = UUID.fromString(user.username)
        val records = mail.id?.let { mailRecordService.getMailRecordByMailId(it) }.orEmpty()
        val canAccess = mail.sender?.id == userId || records.any { it.user?.id == userId }

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
