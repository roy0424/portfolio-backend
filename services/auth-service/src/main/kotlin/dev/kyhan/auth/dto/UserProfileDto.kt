package dev.kyhan.auth.dto

import dev.kyhan.auth.domain.UserProfile
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "User profile information")
data class UserProfileDto(
    @Schema(description = "Profile ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: String,
    @Schema(description = "Owner user ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: String,
    @Schema(description = "Display name", example = "kyhan", nullable = true, maxLength = 20)
    val displayName: String?,
    @Schema(description = "Avatar asset ID", example = "123e4567-e89b-12d3-a456-426614174000", nullable = true)
    val avatarAssetId: String?,
    @Schema(description = "Avatar image URL", example = "https://cdn.example.com/avatar.png", nullable = true)
    val avatarUrl: String?,
    @Schema(description = "Short bio", example = "Building portfolio platform.", nullable = true)
    val bio: String?,
    @Schema(description = "Personal website URL", example = "https://kyhan.dev", nullable = true)
    val website: String?,
    @Schema(description = "Created timestamp", example = "2024-01-01T00:00:00Z")
    val createdAt: Instant,
    @Schema(description = "Last updated timestamp", example = "2024-01-02T00:00:00Z")
    val updatedAt: Instant,
) {
    companion object {
        fun from(
            profile: UserProfile,
            avatarUrl: String? = null,
        ): UserProfileDto =
            UserProfileDto(
                id = profile.id!!.toString(),
                userId = profile.userId.toString(),
                displayName = profile.displayName,
                avatarAssetId = profile.avatarAssetId?.toString(),
                avatarUrl = avatarUrl,
                bio = profile.bio,
                website = profile.website,
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt,
            )
    }
}
