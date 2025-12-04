package dev.kyhan.asset.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table(name = "asset", schema = "asset")
data class Asset(
    @Id
    val id: UUID? = null,
    val userId: UUID,
    val siteId: UUID? = null, // Optional: user-level or site-level asset
    val fileName: String, // Generated filename
    val originalFileName: String, // User's original filename
    val contentType: String, // MIME type
    val fileSize: Long, // Bytes
    val storagePath: String, // S3 key (CDN URL can be computed: {cdnBaseUrl}/{storagePath})
    val storageProvider: String = "R2",
    val visibility: AssetVisibility = AssetVisibility.PRIVATE,
    val status: AssetStatus = AssetStatus.ACTIVE,
    val uploadedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null,
)

enum class AssetStatus {
    ACTIVE,
    PROCESSING,
    FAILED,
    DELETED,
}

enum class AssetVisibility {
    PUBLIC,
    PRIVATE,
}
