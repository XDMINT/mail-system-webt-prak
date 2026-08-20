package de.thm.mni.backend.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import de.thm.mni.backend.user.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "External recipient data to resolve or create a mail contact.")
data class EnsureUserRequest(
    @field:Schema(description = "External recipient email address.", example = "customer@example.org", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Email(message = "Email should be valid")
    @field:NotBlank(message = "Email must not be empty")
    @field:Size(min = 1, max = User.EMAIL_MAX_LENGTH, message = "Email must be between 1 and 255 characters")
    val email: String,
    @field:Schema(description = "Optional first name.", example = "Erika")
    @field:Size(min = 1, max = User.NAME_MAX_LENGTH, message = "First name must be between 1 and 255 characters")
    @field:Pattern(regexp = ".*\\S.*", message = "First name must not be blank")
    val firstName: String? = null,
    @field:Schema(description = "Optional last name.", example = "Mustermann")
    @field:Size(min = 1, max = User.NAME_MAX_LENGTH, message = "Last name must be between 1 and 255 characters")
    @field:Pattern(regexp = ".*\\S.*", message = "Last name must not be blank")
    val lastName: String? = null,
)

