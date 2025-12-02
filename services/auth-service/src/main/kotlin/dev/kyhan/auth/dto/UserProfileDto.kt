package dev.kyhan.auth.dto

data class UserProfileDto(
    val displayName: String?,
    val avatarUrl: String?,
    val bio: String?,
    val location: String?,
    val website: String?,
)
