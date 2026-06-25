package de.thm.mni.backend.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

@Schema(description = "Credentials used to authenticate an existing user.")
data class LoginRequest(
    @field:Schema(
        description = "Email address of the registered user.",
        example = "aallanson@example.com"
    )
    @field:Email(message = "Email should be valid")
    val email: String,

    @field:Schema(
        description = "User password. Must be at least 6 characters long.",
        example = "123456"
    )
    @field:Size(min = 6, message = "Password must be at least 6 characters long")
    val password: String
)
