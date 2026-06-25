package de.thm.mni.backend.user.dto

import de.thm.mni.backend.user.User
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Public user profile returned by the API.")
data class UserDTO(
    @field:Schema(
        description = "Unique user id.",
        example = "6f1f9368-d279-4a6e-993f-f0618767eeb8"
    )
    val id: UUID?,

    @field:Schema(description = "User's first name.", example = "Ameline")
    val firstName: String,

    @field:Schema(description = "User's last name.", example = "Allanson")
    val lastName: String,

    @field:Schema(description = "User's email address.", example = "aallanson@example.com")
    val email: String
)


fun User.toDTO() = UserDTO(
    id = this.id,
    firstName = this.firstName,
    lastName = this.lastName,
    email = this.email
)
