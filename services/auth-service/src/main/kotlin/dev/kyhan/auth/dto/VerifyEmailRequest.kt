package dev.kyhan.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "Request to verify email with code")
data class VerifyEmailRequest(
    @field:NotBlank(message = "Verification code is required")
    @field:Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
    @Schema(description = "6-digit verification code sent to email", example = "123456", required = true, pattern = "^\\d{6}$")
    val code: String,
)
