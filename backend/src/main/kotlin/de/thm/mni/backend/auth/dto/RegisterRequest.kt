package de.thm.mni.backend.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

@Schema(description = "Request payload for creating a new internal user account.")
data class RegisterRequest(
    @field:Schema(description = "User's first name.", example = "Ameline")
    @field:Size(min = 1, message = "First name must not be empty")
    val firstName: String,

    @field:Schema(description = "User's last name.", example = "Allanson")
    @field:Size(min = 1, message = "Last name must not be empty")
    val lastName: String,

    @field:Schema(
        description = "Unique email address used for login.",
        example = "aallanson@example.com"
    )
    @field:Email(message = "Email should be valid")
    val email: String,

    @field:Schema(
        description = "Initial password. Must be at least 6 characters long.",
        example = "123456"
    )
    @field:Size(min = 6, message = "Password must be at least 6 characters long")
    val password: String
)
