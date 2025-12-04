package dev.kyhan.asset.dto

import dev.kyhan.asset.domain.AssetVisibility
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

@Schema(description = "Request to initiate file upload")
data class InitiateUploadRequest(
    @field:NotBlank(message = "Original filename is required")
    @field:Schema(description = "Original filename", example = "my-photo.jpg", required = true)
    val originalFileName: String,
    @field:NotBlank(message = "Content type is required")
    @field:Schema(description = "MIME content type", example = "image/jpeg", required = true)
    val contentType: String,
    @field:Min(1, message = "File size must be greater than 0")
    @field:Schema(description = "File size in bytes", example = "1024000", required = true)
    val fileSize: Long,
    @field:Schema(description = "Optional site ID", example = "123e4567-e89b-12d3-a456-426614174000", nullable = true)
    val siteId: String? = null,
    @field:Schema(description = "Visibility (PUBLIC/PRIVATE)", example = "PRIVATE", defaultValue = "PRIVATE")
    val visibility: AssetVisibility = AssetVisibility.PRIVATE,
)

@Schema(description = "Response with presigned upload URL")
data class InitiateUploadResponse(
    @field:Schema(description = "Asset ID (for completion call)", example = "123e4567-e89b-12d3-a456-426614174000")
    val assetId: String,
    @field:Schema(description = "Presigned upload URL (PUT file here)", example = "https://r2.cloudflare.com/...")
    val uploadUrl: String,
    @field:Schema(
        description = "CDN URL for accessing after upload (only for PUBLIC assets)",
        example = "https://assets.example.com/public/uuid/file.jpg",
        nullable = true,
    )
    val cdnUrl: String?,
    @field:Schema(description = "Asset visibility", example = "PRIVATE")
    val visibility: AssetVisibility,
    @field:Schema(description = "Upload URL expiration", example = "2024-01-01T00:15:00Z")
    val expiresAt: Instant,
)
