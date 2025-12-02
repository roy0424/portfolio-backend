package dev.kyhan.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Email verification operation response")
data class EmailVerificationResponse(
    @Schema(description = "Result message", example = "Verification email sent successfully")
    val message: String,
    @Schema(description = "Email address (only included in registration response)", example = "user@example.com", nullable = true)
    val email: String?,
)
