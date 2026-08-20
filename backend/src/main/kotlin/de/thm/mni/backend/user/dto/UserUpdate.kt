package de.thm.mni.backend.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

@Schema(description = "Request payload for updating the authenticated user's profile.")
data class UserUpdate(
    @field:Schema(description = "Updated first name.", example = "Ameline")
    @field:Size(min = 1, message = "First name must not be empty")
    val firstName: String,

    @field:Schema(description = "Updated last name.", example = "Allanson")
    @field:Size(min = 1, message = "Last name must not be empty")
    val lastName: String,

    @field:Schema(
        description = "Updated email address. Must not already be used by another user.",
        example = "ameline.allanson@example.com"
    )
    @field:Email(message = "Email should be valid")
    val email: String
)
