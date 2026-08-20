package de.thm.mni.backend.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Generic page response wrapper.")
data class PageResponse<T>(
    @field:Schema(description = "Items on the current page.")
    val content: List<T>,

    @field:Schema(description = "Zero-based page index.", example = "0")
    val page: Int,

    @field:Schema(description = "Effective page size.", example = "25")
    val size: Int,

    @field:Schema(description = "Total number of matching items.", example = "142")
    val totalElements: Long,

    @field:Schema(description = "Total number of available pages.", example = "6")
    val totalPages: Int,

    @field:Schema(description = "Whether another page exists after the current page.", example = "true")
    val hasNext: Boolean,
)
