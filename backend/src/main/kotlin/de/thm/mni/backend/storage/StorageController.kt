package de.thm.mni.backend.storage

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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.util.UUID
import de.thm.mni.backend.openapi.DefaultApiResponses
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag


@RestController
@RequestMapping("/api/attachments")
@Tag(name = "Attachment", description = "Download authorized mail attachments or preview safe raster images.")
@DefaultApiResponses
class StorageController(
    private val attachmentDownloadService: AttachmentDownloadService,
    private val currentUserService: CurrentUserService,
) {
    @Operation(
        operationId = "getAttachment",
        summary = "Download or preview an attachment",
        description = "Returns an attachment as a download. Safe raster images can be requested as an inline preview.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Attachment content returned",
        content = [Content(
            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            schema = Schema(type = "string", format = "binary"),
        )],
    )
    @ApiResponse(responseCode = "404", description = "Attachment not found or not accessible")
    @GetMapping("/{attachmentId}")
    fun getAttachment(
        @PathVariable attachmentId: UUID,
        @Parameter(description = "Return safe raster images inline instead of forcing a download.")
        @RequestParam(defaultValue = "false") preview: Boolean,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Resource> {
        val userId = currentUserService.getOrProvision(jwt).id!!
        val download = attachmentDownloadService.load(attachmentId, userId, preview)
        val disposition = if (download.inline) ContentDisposition.inline() else ContentDisposition.attachment()

        return ResponseEntity.ok()
            .contentType(download.contentType)
            .contentLength(download.contentLength)
            .header("X-Content-Type-Options", "nosniff")
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                disposition
                    .filename(download.fileName, StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .body(download.resource)
    }
}
