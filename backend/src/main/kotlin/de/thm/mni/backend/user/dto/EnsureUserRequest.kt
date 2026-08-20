package de.thm.mni.backend.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request payload for finding or creating an external contact user.")
data class EnsureUserRequest(
    @field:Schema(
        description = "Email address of the external contact. The server normalizes it before lookup.",
        example = "customer@example.com"
    )
    @field:Email(message = "Email should be valid")
    @field:NotBlank(message = "Email must not be empty")
    val email: String,

    @field:Schema(
        description = "Optional first name. If omitted or blank, the server uses `External`.",
        example = "Chris"
    )
    val firstName: String? = null,

    @field:Schema(
        description = "Optional last name. If omitted or blank, the server uses `User`.",
        example = "Customer"
    )
    val lastName: String? = null,
)
