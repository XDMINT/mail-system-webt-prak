package de.thm.mni.backend.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class EnsureUserRequest(
    @field:Email(message = "Email should be valid")
    @field:NotBlank(message = "Email must not be empty")
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
)

