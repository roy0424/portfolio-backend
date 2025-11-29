package dev.kyhan.common.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)