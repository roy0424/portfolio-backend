package dev.kyhan.auth.dto

import jakarta.validation.constraints.NotBlank

data class AuthResponse(
    val userId: String,
    val email: String?,
    val emailVerified: Boolean,
    val accessToken: String,
    val refreshToken: String,
    val profile: UserProfileDto?
)

data class UserProfileDto(
    val displayName: String?,
    val avatarUrl: String?,
    val bio: String?,
    val location: String?,
    val website: String?
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)