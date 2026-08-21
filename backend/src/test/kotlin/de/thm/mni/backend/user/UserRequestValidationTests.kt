package de.thm.mni.backend.user

import de.thm.mni.backend.user.dto.EnsureUserRequest
import de.thm.mni.backend.user.dto.UserUpdate
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserRequestValidationTests {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `user updates reject blank and oversized persisted fields`() {
        val blankName = validator.validate(UserUpdate("   ", "Mustermann", "erika@example.org"))
        assertTrue(blankName.any { it.propertyPath.toString() == "firstName" })

        val oversized = validator.validate(
            UserUpdate(
                "x".repeat(User.NAME_MAX_LENGTH + 1),
                "x".repeat(User.NAME_MAX_LENGTH + 1),
                "x".repeat(User.EMAIL_MAX_LENGTH + 1),
            )
        )
        assertEquals(setOf("firstName", "lastName", "email"), oversized.map { it.propertyPath.toString() }.toSet())
    }

    @Test
    fun `external contact names remain optional but respect persistence limits`() {
        assertTrue(validator.validate(EnsureUserRequest("customer@example.org")).isEmpty())

        val blankNames = validator.validate(
            EnsureUserRequest(email = "customer@example.org", firstName = "", lastName = "   ")
        )
        assertEquals(setOf("firstName", "lastName"), blankNames.map { it.propertyPath.toString() }.toSet())

        val oversized = validator.validate(
            EnsureUserRequest(
                email = "customer@example.org",
                firstName = "x".repeat(User.NAME_MAX_LENGTH + 1),
                lastName = "x".repeat(User.NAME_MAX_LENGTH + 1),
            )
        )
        assertEquals(setOf("firstName", "lastName"), oversized.map { it.propertyPath.toString() }.toSet())
    }
}
