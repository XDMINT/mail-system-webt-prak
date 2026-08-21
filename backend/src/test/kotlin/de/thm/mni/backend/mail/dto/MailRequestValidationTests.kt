package de.thm.mni.backend.mail.dto

import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class MailRequestValidationTests {
    private val validator = Validation.buildDefaultValidatorFactory().validator
    private val recipientId = UUID.randomUUID()

    @Test
    fun `create request rejects blank subject and content`() {
        val request = MailRequest(
            subject = "   ",
            content = "\t",
            toIds = mutableListOf(recipientId),
            ccIds = mutableListOf(),
            bccIds = mutableListOf(),
        )

        val invalidProperties = validator.validate(request).map { it.propertyPath.toString() }.toSet()

        assertTrue("subject" in invalidProperties)
        assertTrue("content" in invalidProperties)
    }

    @Test
    fun `update request rejects blank subject and content`() {
        val request = MailUpdateRequest(
            subject = "   ",
            content = "\n",
            toIds = mutableListOf(recipientId),
            ccIds = mutableListOf(),
            bccIds = mutableListOf(),
        )

        val invalidProperties = validator.validate(request).map { it.propertyPath.toString() }.toSet()

        assertTrue("subject" in invalidProperties)
        assertTrue("content" in invalidProperties)
    }
}
