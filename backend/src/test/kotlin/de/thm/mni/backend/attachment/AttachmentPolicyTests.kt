package de.thm.mni.backend.attachment

import de.thm.mni.backend.error.AttachmentTooLargeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import java.io.ByteArrayInputStream

class AttachmentPolicyTests {
    private val policy = AttachmentPolicy(DataSize.ofBytes(4), DataSize.ofBytes(8))

    @Test
    fun `normalizes untrusted file metadata and restricts previews to raster images`() {
        assertEquals("evil.png", policy.normalizeFileName("../folder\\evil\r\n.png"))
        assertEquals("image/png", policy.normalizeContentType("image/png; charset=UTF-8"))
        assertEquals(null, policy.normalizeContentType("not a media type"))
        assertEquals(null, policy.normalizeContentType("image/${"x".repeat(256)}"))
        assertTrue(policy.isPreviewable("image/png"))
        assertFalse(policy.isPreviewable("image/svg+xml"))
        assertFalse(policy.isPreviewable("text/html"))
    }

    @Test
    fun `bounded reads reject attachments above the configured limit`() {
        assertThrows(AttachmentTooLargeException::class.java) {
            policy.readLimited(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5)))
        }
    }
}
