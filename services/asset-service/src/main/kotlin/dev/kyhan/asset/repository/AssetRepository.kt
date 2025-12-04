package dev.kyhan.asset.repository

import dev.kyhan.asset.domain.Asset
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface AssetRepository : ReactiveCrudRepository<Asset, UUID> {
    /**
     * Find active (non-deleted) asset by ID
     */
    @Query("SELECT * FROM asset.asset WHERE id = :assetId AND deleted_at IS NULL")
    fun findActiveById(assetId: UUID): Mono<Asset>

    /**
     * Find all active assets by user ID with pagination
     */
    @Query(
        """
        SELECT * FROM asset.asset
        WHERE user_id = :userId AND deleted_at IS NULL
        ORDER BY uploaded_at DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    fun findActiveByUserId(
        userId: UUID,
        limit: Int,
        offset: Long,
    ): Flux<Asset>

    /**
     * Count active assets by user ID
     */
    @Query("SELECT COUNT(*) FROM asset.asset WHERE user_id = :userId AND deleted_at IS NULL")
    fun countActiveByUserId(userId: UUID): Mono<Long>

    /**
     * Find all active assets by site ID
     */
    @Query("SELECT * FROM asset.asset WHERE site_id = :siteId AND deleted_at IS NULL ORDER BY uploaded_at DESC")
    fun findActiveBySiteId(siteId: UUID): Flux<Asset>

    /**
     * Find active assets by user ID and content type prefix (e.g., "image/")
     */
    @Query(
        """
        SELECT * FROM asset.asset
        WHERE user_id = :userId
        AND content_type LIKE :contentTypePrefix
        AND deleted_at IS NULL
        ORDER BY uploaded_at DESC
    """,
    )
    fun findActiveByUserIdAndContentType(
        userId: UUID,
        contentTypePrefix: String,
    ): Flux<Asset>

    /**
     * Calculate total storage used by user (in bytes)
     */
    @Query("SELECT COALESCE(SUM(file_size), 0) FROM asset.asset WHERE user_id = :userId AND deleted_at IS NULL")
    fun getTotalStorageByUserId(userId: UUID): Mono<Long>
}
