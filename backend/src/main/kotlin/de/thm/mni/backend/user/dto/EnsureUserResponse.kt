package de.thm.mni.backend.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "External contact user returned after lookup or creation.")
data class EnsureUserResponse(
    @field:Schema(
        description = "Unique contact user id.",
        example = "4ff9c0df-c0f5-4fa2-a7d5-6e517f22186c"
    )
    val id: UUID?,

    @field:Schema(description = "Contact first name.", example = "External")
    val firstName: String,

    @field:Schema(description = "Contact last name.", example = "User")
    val lastName: String,

    @field:Schema(description = "Contact email address.", example = "customer@example.com")
    val email: String,
)
