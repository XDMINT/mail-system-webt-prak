package de.thm.mni.backend.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import de.thm.mni.backend.user.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Editable fields of the authenticated user's local profile.")
data class UserUpdate(
    @field:Schema(description = "First name.", example = "Erika", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "First name must not be blank")
    @field:Size(min = 1, max = User.NAME_MAX_LENGTH, message = "First name must be between 1 and 255 characters")
    val firstName: String,
    @field:Schema(description = "Last name.", example = "Mustermann", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "Last name must not be blank")
    @field:Size(min = 1, max = User.NAME_MAX_LENGTH, message = "Last name must be between 1 and 255 characters")
    val lastName: String,
    @field:Schema(description = "Unique email address.", example = "erika@example.org", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "Email must not be blank")
    @field:Email(message = "Email should be valid")
    @field:Size(min = 1, max = User.EMAIL_MAX_LENGTH, message = "Email must be between 1 and 255 characters")
    val email: String
)
