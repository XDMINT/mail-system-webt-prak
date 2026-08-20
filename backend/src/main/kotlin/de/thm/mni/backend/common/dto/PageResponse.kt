package de.thm.mni.backend.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A page of API results with pagination metadata.")
data class PageResponse<T>(
    @field:Schema(description = "Items on the current page.", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: List<T>,
    @field:Schema(description = "Zero-based page number.", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    val page: Int,
    @field:Schema(description = "Maximum number of items per page.", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
    val size: Int,
    @field:Schema(description = "Total number of matching items.", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    val totalElements: Long,
    @field:Schema(description = "Total number of pages.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    val totalPages: Int,
    @field:Schema(description = "Whether another page is available.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    val hasNext: Boolean,
)
