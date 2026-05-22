package de.thm.mni.backend.user.dto

data class EnsureUserRequest(
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
)

