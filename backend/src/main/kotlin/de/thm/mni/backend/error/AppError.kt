package de.thm.mni.backend.error

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Standard error response returned by the API.")
data class AppError(
    @field:Schema(description = "HTTP status code.", example = "404", requiredMode = Schema.RequiredMode.REQUIRED)
    val status: Int,
    @field:Schema(description = "Human-readable error description.", example = "Mail not found")
    val message: String?,
)
