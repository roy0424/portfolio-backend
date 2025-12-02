package dev.kyhan.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "Request to update the current user's profile")
data class UpdateUserProfileRequest(
    @field:Size(max = 20, message = "Display name must be at most 20 characters")
    @Schema(description = "Display name", example = "kyhan", maxLength = 20)
    val displayName: String? = null,
    @field:Pattern(
        regexp = "^(https?://.*)?$",
        message = "Avatar URL must start with http:// or https://",
    )
    @Schema(description = "Avatar image URL", example = "https://cdn.example.com/avatar.png")
    val avatarUrl: String? = null,
    @field:Size(max = 500, message = "Bio must be at most 500 characters")
    @Schema(description = "Short bio", example = "Building portfolio platform.", maxLength = 500)
    val bio: String? = null,
    @field:Pattern(
        regexp = "^(https?://.*)?$",
        message = "Website must start with http:// or https://",
    )
    @Schema(description = "Personal website URL", example = "https://kyhan.dev")
    val website: String? = null,
)
