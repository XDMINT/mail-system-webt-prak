package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.attachment.AttachmentPolicy
import de.thm.mni.backend.attachment.AttachmentRepository
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.user.CurrentUserService
import de.thm.mni.backend.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.util.unit.DataSize
import java.time.Instant
import java.util.Optional
import java.util.UUID

class StorageControllerTests {
    private val fileStorageService = Mockito.mock(FileStorageService::class.java)
    private val attachmentRepository = Mockito.mock(AttachmentRepository::class.java)
    private val mailRecordService = Mockito.mock(MailRecordService::class.java)
    private val currentUserService = Mockito.mock(CurrentUserService::class.java)
    private val policy = AttachmentPolicy(DataSize.ofMegabytes(1), DataSize.ofMegabytes(10))
    private val attachmentDownloadService = AttachmentDownloadService(
        attachmentRepository,
        fileStorageService,
        mailRecordService,
        policy,
    )
    private val controller = StorageController(
        attachmentDownloadService,
        currentUserService,
    )
    private val authenticatedJwt = jwt()

    @Test
    fun `regular download is forced to attachment with nosniff`() {
        val attachment = externalAttachment("document.html", "text/html")
        arrangeAccessible(attachment)

        val response = controller.getAttachment(attachment.id!!, false, authenticatedJwt)

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.headers.contentType)
        assertEquals("nosniff", response.headers.getFirst("X-Content-Type-Options"))
        assertTrue(response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)!!.startsWith("attachment"))
    }

    @Test
    fun `safe raster image can be returned as inline preview`() {
        val attachment = externalAttachment("preview.png", "image/png")
        arrangeAccessible(attachment)

        val response = controller.getAttachment(attachment.id!!, true, authenticatedJwt)

        assertEquals(MediaType.IMAGE_PNG, response.headers.contentType)
        assertTrue(response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)!!.startsWith("inline"))
    }

    @Test
    fun `svg preview request remains a download`() {
        val attachment = externalAttachment("vector.svg", "image/svg+xml")
        arrangeAccessible(attachment)

        val response = controller.getAttachment(attachment.id!!, true, authenticatedJwt)

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.headers.contentType)
        assertTrue(response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)!!.startsWith("attachment"))
    }

    @Test
    fun `inaccessible attachment is hidden as not found`() {
        val attachment = externalAttachment("private.txt", "text/plain")
        attachment.mail!!.source = MailSource.INTERN
        val currentUser = User("Current", "User", "current@example.org").apply { id = UUID.randomUUID() }
        Mockito.`when`(attachmentRepository.findById(attachment.id!!)).thenReturn(Optional.of(attachment))
        Mockito.`when`(currentUserService.getOrProvision(authenticatedJwt)).thenReturn(currentUser)
        Mockito.`when`(mailRecordService.getMailRecordByMailId(attachment.mail!!.id!!)).thenReturn(emptyList())

        assertThrows(ResourceNotFoundException::class.java) {
            controller.getAttachment(attachment.id!!, false, authenticatedJwt)
        }
        Mockito.verifyNoInteractions(fileStorageService)
    }

    private fun arrangeAccessible(attachment: Attachment) {
        val currentUser = User("Current", "User", "current@example.org").apply { id = UUID.randomUUID() }
        Mockito.`when`(attachmentRepository.findById(attachment.id!!)).thenReturn(Optional.of(attachment))
        Mockito.`when`(currentUserService.getOrProvision(authenticatedJwt)).thenReturn(currentUser)
        Mockito.`when`(mailRecordService.getMailRecordByMailId(attachment.mail!!.id!!)).thenReturn(emptyList())
        Mockito.`when`(fileStorageService.load(attachment.path)).thenReturn(
            StoredFile(ByteArrayResource(byteArrayOf(1, 2)), attachment.mimeType, 2)
        )
    }

    private fun externalAttachment(fileName: String, mimeType: String): Attachment {
        val sender = User("External", "Sender", "external@example.org").apply { id = UUID.randomUUID() }
        val mail = Mail(sender, "Subject", "Body", mutableListOf()).apply {
            id = UUID.randomUUID()
            source = MailSource.EXTERN
        }
        return Attachment().apply {
            id = UUID.randomUUID()
            this.fileName = fileName
            this.mimeType = mimeType
            size = 2
            path = "attachments/object"
            this.mail = mail
        }
    }

    private fun jwt(): Jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build()
}
