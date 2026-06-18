package de.thm.mni.backend.user.dto

import java.util.UUID

data class EnsureUserResponse(
    val id: UUID?,
    val firstName: String,
    val lastName: String,
    val email: String,
)
