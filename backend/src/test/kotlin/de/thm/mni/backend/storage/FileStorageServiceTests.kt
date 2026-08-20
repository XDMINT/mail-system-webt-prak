package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.AttachmentPolicy
import de.thm.mni.backend.error.AttachmentTooLargeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockMultipartFile
import org.springframework.util.unit.DataSize

class FileStorageServiceTests {
    private val repository = Mockito.mock(FileStorageRepository::class.java)
    private val policy = AttachmentPolicy(DataSize.ofBytes(4), DataSize.ofBytes(8))
    private val service = FileStorageService(repository, policy)

    @Test
    fun `rejects an oversized multipart file before storage`() {
        val file = MockMultipartFile("attachments", "large.bin", "application/octet-stream", ByteArray(5))

        assertThrows(AttachmentTooLargeException::class.java) { service.saveFile(file) }
        Mockito.verifyNoInteractions(repository)
    }

    @Test
    fun `normalizes file name and content type before storage`() {
        val stored = StoredAttachment(2, "image.png", "image/png", "attachments/id")
        Mockito.`when`(repository.saveFile("image.png", "image/png", byteArrayOf(1, 2))).thenReturn(stored)

        val result = service.saveFile("../image.png", "image/png; charset=UTF-8", byteArrayOf(1, 2))

        assertEquals(stored, result)
    }
}
