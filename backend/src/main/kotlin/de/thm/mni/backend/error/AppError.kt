package de.thm.mni.backend.error

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Standard error response returned by the backend.")
class AppError {
    @field:Schema(description = "HTTP status code.", example = "404")
    val status: Int

    @field:Schema(description = "Human-readable error message.", example = "Mail not found")
    val message: String?

    constructor(status: Int, message: String?) {
        this.status = status
        this.message = message
    }
}
