package dev.kyhan.asset.dto

import dev.kyhan.asset.domain.Asset
import dev.kyhan.asset.domain.AssetStatus
import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.common.dto.PageResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Asset metadata")
data class AssetDto(
    @field:Schema(description = "Asset ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: String,
    @field:Schema(description = "Owner user ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: String,
    @field:Schema(description = "Site ID (optional)", example = "123e4567-e89b-12d3-a456-426614174000", nullable = true)
    val siteId: String?,
    @field:Schema(description = "Generated filename", example = "abc-456.jpg")
    val fileName: String,
    @field:Schema(description = "Original uploaded filename", example = "my-photo.jpg")
    val originalFileName: String,
    @field:Schema(description = "MIME content type", example = "image/jpeg")
    val contentType: String,
    @field:Schema(description = "File size in bytes", example = "1024000")
    val fileSize: Long,
    @field:Schema(description = "Asset visibility", example = "PUBLIC")
    val visibility: AssetVisibility,
    @field:Schema(description = "Asset status", example = "ACTIVE")
    val status: AssetStatus,
    @field:Schema(description = "Upload timestamp", example = "2024-01-01T00:00:00Z")
    val uploadedAt: Instant,
) {
    companion object {
        fun from(asset: Asset): AssetDto =
            AssetDto(
                id = asset.id.toString(),
                userId = asset.userId.toString(),
                siteId = asset.siteId?.toString(),
                fileName = asset.fileName,
                originalFileName = asset.originalFileName,
                contentType = asset.contentType,
                fileSize = asset.fileSize,
                visibility = asset.visibility,
                status = asset.status,
                uploadedAt = asset.uploadedAt,
            )
    }
}

@Schema(description = "Paginated asset list response")
data class AssetListResponse(
    @field:Schema(description = "Pagination information including asset list")
    val page: PageResponse<AssetDto>,
)

@Schema(description = "Download URL response")
data class DownloadUrlResponse(
    @field:Schema(description = "Download URL (CDN or presigned)", example = "https://assets.example.com/public/uuid/file.jpg")
    val url: String,
    @field:Schema(description = "URL expiration timestamp", example = "2024-01-01T01:00:00Z", nullable = true)
    val expiresAt: Instant?,
)
