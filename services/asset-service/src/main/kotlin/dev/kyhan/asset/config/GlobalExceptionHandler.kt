package dev.kyhan.asset.config

import dev.kyhan.common.dto.ApiResponse
import dev.kyhan.common.dto.ErrorResponse
import dev.kyhan.common.exception.BusinessException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "Business exception: ${ex.message}" }
        return ResponseEntity
            .status(ex.status)
            .body(
                ApiResponse.error(
                    ErrorResponse(
                        code = ex.code,
                        message = ex.message,
                    ),
                ),
            )
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(ex: WebExchangeBindException): ResponseEntity<ApiResponse<Nothing>> {
        val errors =
            ex.bindingResult.fieldErrors.associate {
                it.field to (it.defaultMessage ?: "Invalid value")
            }

        logger.warn { "Validation error: $errors" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponse.error(
                    ErrorResponse(
                        code = "1001",
                        message = "Validation failed",
                        details = errors,
                    ),
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error(ex) { "Unexpected error: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiResponse.error(
                    ErrorResponse(
                        code = "1000",
                        message = "Internal server error",
                    ),
                ),
            )
    }
}
