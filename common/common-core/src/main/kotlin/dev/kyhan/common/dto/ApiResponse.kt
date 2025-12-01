package dev.kyhan.common.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun <T> error(error: ErrorResponse): ApiResponse<T> = ApiResponse(success = false, error = error)

        fun <T> error(
            code: String,
            message: String,
        ): ApiResponse<T> = ApiResponse(success = false, error = ErrorResponse(code, message))
    }
}
