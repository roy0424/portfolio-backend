package dev.kyhan.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Authentication response containing tokens and user info")
data class AuthResponse(
    @Schema(description = "User unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: String,
    @Schema(description = "User email address", example = "user@example.com", nullable = true)
    val email: String?,
    @Schema(description = "JWT access token for API authentication")
    val accessToken: String,
    @Schema(description = "JWT refresh token for obtaining new access tokens")
    val refreshToken: String,
    @Schema(description = "User profile information", nullable = true)
    val profile: UserProfileDto?,
)
