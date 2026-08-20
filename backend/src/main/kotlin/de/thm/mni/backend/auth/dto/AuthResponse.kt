package de.thm.mni.backend.auth.dto

import de.thm.mni.backend.user.dto.UserDTO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Authentication result containing the user profile and a JWT bearer token.")
data class AuthResponse(
    @field:Schema(description = "Authenticated user profile.")
    val user: UserDTO,

    @field:Schema(
        description = "JWT bearer token. Send this token as `Authorization: Bearer <token>` for protected endpoints.",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val token: String,
)
