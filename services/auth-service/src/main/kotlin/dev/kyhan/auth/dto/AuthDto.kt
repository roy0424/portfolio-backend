package dev.kyhan.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AuthResponse(
    val userId: String,
    val email: String?,
    val accessToken: String,
    val refreshToken: String,
    val profile: UserProfileDto?,
)

data class UserProfileDto(
    val displayName: String?,
    val avatarUrl: String?,
    val bio: String?,
    val location: String?,
    val website: String?,
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String,
)

data class RegisterEmailRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
)

data class VerifyEmailRequest(
    @field:NotBlank(message = "Verification code is required")
    @field:Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
    val code: String,
)

data class EmailVerificationResponse(
    val message: String,
    val email: String?,
)

data class ResendVerificationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
)
