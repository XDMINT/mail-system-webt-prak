package de.thm.mni.backend.user.dto

import de.thm.mni.backend.user.User
import java.util.UUID
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Resolved external mail contact.")
data class EnsureUserResponse(
    @field:Schema(description = "Contact identifier.", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: UUID,
    @field:Schema(description = "First name.", example = "Erika", minLength = 1, maxLength = User.NAME_MAX_LENGTH, requiredMode = Schema.RequiredMode.REQUIRED)
    val firstName: String,
    @field:Schema(description = "Last name.", example = "Mustermann", minLength = 1, maxLength = User.NAME_MAX_LENGTH, requiredMode = Schema.RequiredMode.REQUIRED)
    val lastName: String,
    @field:Schema(description = "Email address.", example = "customer@example.org", minLength = 1, maxLength = User.EMAIL_MAX_LENGTH, requiredMode = Schema.RequiredMode.REQUIRED)
    val email: String,
)
