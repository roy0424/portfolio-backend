package dev.kyhan.common.dto

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun <T> of(
            content: List<T>,
            page: Int,
            size: Int,
            totalElements: Long
        ): PageResponse<T> {
            val totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 0
            return PageResponse(
                content = content,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages,
                hasNext = page < totalPages - 1,
                hasPrevious = page > 0
            )
        }
    }
}