package dev.kyhan.asset.service

import dev.kyhan.asset.config.AssetProperties
import dev.kyhan.asset.domain.Asset
import dev.kyhan.asset.domain.AssetStatus
import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.asset.dto.AssetDto
import dev.kyhan.asset.dto.AssetListResponse
import dev.kyhan.asset.dto.DownloadUrlResponse
import dev.kyhan.asset.dto.InitiateUploadRequest
import dev.kyhan.asset.dto.InitiateUploadResponse
import dev.kyhan.asset.repository.AssetRepository
import dev.kyhan.common.dto.PageResponse
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import dev.kyhan.common.exception.NotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class AssetService(
    private val assetRepository: AssetRepository,
    private val r2StorageService: R2StorageService,
    private val assetProperties: AssetProperties,
    private val eventPublisher: AssetEventPublisher,
    private val r2Properties: dev.kyhan.asset.config.R2Properties,
) {
    /**
     * Initiate file upload - returns presigned URL
     */
    suspend fun initiateUpload(
        userId: UUID,
        siteId: UUID?,
        request: InitiateUploadRequest,
    ): InitiateUploadResponse {
        // Validate file size
        if (request.fileSize > assetProperties.maxFileSize) {
            throw InvalidInputException(
                ErrorCode.FILE_TOO_LARGE,
                "File size ${request.fileSize} exceeds maximum ${assetProperties.maxFileSize} bytes",
            )
        }

        // Validate content type if restrictions exist
        if (assetProperties.allowedContentTypes.isNotEmpty()) {
            val isAllowed =
                assetProperties.allowedContentTypes.any {
                    request.contentType.startsWith(it, ignoreCase = true)
                }
            if (!isAllowed) {
                throw InvalidInputException(
                    ErrorCode.INVALID_FILE_TYPE,
                    "File type not allowed: ${request.contentType}",
                )
            }
        }

        logger.info { "Initiating upload for user $userId: ${request.originalFileName}" }

        // Create Asset entity with PROCESSING status (to get assetId)
        val asset =
            Asset(
                userId = userId,
                siteId = siteId,
                fileName = "", // Will be set after assetId is generated
                originalFileName = request.originalFileName,
                contentType = request.contentType,
                fileSize = request.fileSize,
                storagePath = "", // Will be set after assetId is generated
                visibility = request.visibility,
                status = AssetStatus.PROCESSING,
            )

        // Save to database to generate assetId
        val savedAsset = assetRepository.save(asset).awaitSingle()
        val assetId = savedAsset.id!!

        // Generate storage path with assetId
        val storagePath =
            r2StorageService.generateStoragePath(
                userId,
                assetId,
                request.originalFileName,
                request.visibility,
            )

        // Update asset with storage path
        val updatedAsset =
            savedAsset.copy(
                fileName = storagePath.substringAfterLast('/'),
                storagePath = storagePath,
            )
        assetRepository.save(updatedAsset).awaitSingle()

        // Generate presigned upload URL
        val uploadUrl =
            r2StorageService.generatePresignedUploadUrl(
                storagePath,
                request.contentType,
                900, // 15 minutes
            )

        val expiresAt = Instant.now().plusSeconds(900)

        logger.info { "Upload initiated for asset $assetId" }

        // Build CDN URL (only for PUBLIC assets)
        val cdnUrl =
            if (request.visibility == AssetVisibility.PUBLIC) {
                r2StorageService.buildCdnUrl(storagePath)
            } else {
                null
            }

        return InitiateUploadResponse(
            assetId = assetId.toString(),
            uploadUrl = uploadUrl,
            cdnUrl = cdnUrl,
            visibility = request.visibility,
            expiresAt = expiresAt,
        )
    }

    /**
     * Complete file upload after client uploads to presigned URL
     */
    suspend fun completeUpload(
        assetId: UUID,
        userId: UUID,
    ): AssetDto {
        val asset =
            assetRepository.findById(assetId).awaitSingleOrNull()
                ?: throw NotFoundException(ErrorCode.ASSET_NOT_FOUND)

        // Verify ownership
        if (asset.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN, "Access denied")
        }

        // Verify status is PROCESSING
        if (asset.status != AssetStatus.PROCESSING) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "Asset is not in PROCESSING status: ${asset.status}",
            )
        }

        // Optional: Verify file exists in R2
        val fileExists = r2StorageService.checkFileExists(asset.storagePath)
        if (!fileExists) {
            logger.warn { "File not found in R2 for asset $assetId: ${asset.storagePath}" }
            throw BusinessException(ErrorCode.ASSET_NOT_FOUND, "File not found in storage")
        }

        // Update status to ACTIVE
        val completedAsset = asset.copy(status = AssetStatus.ACTIVE)
        val savedAsset = assetRepository.save(completedAsset).awaitSingle()

        // Publish event
        eventPublisher.publishAssetUploaded(savedAsset)

        logger.info { "Upload completed for asset $assetId" }

        return AssetDto.from(savedAsset)
    }

    /**
     * Get asset by ID
     */
    fun getAsset(
        assetId: UUID,
        userId: UUID,
    ): Mono<AssetDto> =
        assetRepository
            .findActiveById(assetId)
            .switchIfEmpty(Mono.error(NotFoundException(ErrorCode.ASSET_NOT_FOUND)))
            .flatMap { asset ->
                // Verify ownership
                if (asset.userId != userId) {
                    Mono.error(BusinessException(ErrorCode.FORBIDDEN, "Access denied"))
                } else {
                    Mono.just(AssetDto.from(asset))
                }
            }

    /**
     * List user's assets with pagination
     */
    fun listAssets(
        userId: UUID,
        page: Int = 0,
        size: Int = 20,
    ): Mono<AssetListResponse> {
        val offset = (page * size).toLong()

        val assetsFlux =
            assetRepository
                .findActiveByUserId(userId, size, offset)
                .map { asset -> AssetDto.from(asset) }

        val countMono = assetRepository.countActiveByUserId(userId)

        return Mono
            .zip(assetsFlux.collectList(), countMono)
            .map { tuple ->
                val assets = tuple.t1
                val total = tuple.t2

                AssetListResponse(
                    page =
                        PageResponse.of(
                            content = assets,
                            page = page,
                            size = size,
                            totalElements = total,
                        ),
                )
            }
    }

    /**
     * Delete asset (soft delete)
     */
    suspend fun deleteAsset(
        assetId: UUID,
        userId: UUID,
    ) {
        val asset =
            assetRepository.findActiveById(assetId).awaitSingleOrNull()
                ?: throw NotFoundException(ErrorCode.ASSET_NOT_FOUND)

        // Verify ownership
        if (asset.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN, "Access denied")
        }

        // Soft delete in database
        val deletedAsset =
            asset.copy(
                status = AssetStatus.DELETED,
                deletedAt = Instant.now(),
            )

        assetRepository.save(deletedAsset).awaitSingle()

        // Delete from R2 (async, don't wait)
        try {
            r2StorageService.deleteFile(asset.storagePath)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to delete file from R2, but soft delete succeeded" }
        }

        // Publish event
        eventPublisher.publishAssetDeleted(deletedAsset)

        logger.info { "Asset deleted: $assetId" }
    }

    /**
     * Get download URL for asset
     */
    suspend fun getDownloadUrl(
        assetId: UUID,
        userId: UUID,
    ): DownloadUrlResponse {
        val asset =
            assetRepository.findActiveById(assetId).awaitSingleOrNull()
                ?: throw NotFoundException(ErrorCode.ASSET_NOT_FOUND)

        // Verify ownership
        if (asset.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN, "Access denied")
        }

        return when (asset.visibility) {
            AssetVisibility.PUBLIC -> {
                // PUBLIC: Return permanent CDN URL
                // Storage path includes original filename: {visibility}/{userId}/{assetId}/{originalFileName}
                // CDN URL will show original filename in browser
                val cdnUrl = r2StorageService.buildCdnUrl(asset.storagePath)
                DownloadUrlResponse(
                    url = cdnUrl,
                    expiresAt = null, // CDN URLs don't expire
                )
            }
            AssetVisibility.PRIVATE -> {
                // PRIVATE: Generate signed URL via CDN with 1 hour expiration
                // Uses HMAC-SHA256 signature verified by Cloudflare Worker
                // Storage path already includes original filename: {visibility}/{userId}/{assetId}/{originalFileName}
                val signedUrl =
                    r2StorageService.generateSignedDownloadUrl(
                        storagePath = asset.storagePath,
                        expirationSeconds = 3600, // 1 hour
                    )
                val expiresAt = Instant.now().plusSeconds(3600)

                DownloadUrlResponse(
                    url = signedUrl,
                    expiresAt = expiresAt,
                )
            }
        }
    }

    /**
     * Update asset visibility (called by Kafka consumer or REST API)
     */
    suspend fun updateVisibility(
        assetId: UUID,
        userId: UUID,
        newVisibility: AssetVisibility,
    ): AssetDto {
        val asset =
            assetRepository.findActiveById(assetId).awaitSingleOrNull()
                ?: throw NotFoundException(ErrorCode.ASSET_NOT_FOUND)

        // Verify ownership
        if (asset.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN, "Access denied")
        }

        // Validate status is ACTIVE
        if (asset.status != AssetStatus.ACTIVE) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "Cannot change visibility for non-active asset",
            )
        }

        // If visibility unchanged, return current
        if (asset.visibility == newVisibility) {
            return AssetDto.from(asset)
        }

        val oldVisibility = asset.visibility

        logger.info { "Changing visibility for asset $assetId: $oldVisibility → $newVisibility" }

        // Generate new storage path with new visibility
        val newStoragePath =
            r2StorageService.generateStoragePath(
                asset.userId,
                assetId,
                asset.originalFileName,
                newVisibility,
            )

        // Copy file to new path
        r2StorageService.copyFile(asset.storagePath, newStoragePath)

        // Delete old file
        r2StorageService.deleteFile(asset.storagePath)

        // Update asset (cdnUrl field removed from domain)
        val updatedAsset =
            asset.copy(
                visibility = newVisibility,
                storagePath = newStoragePath,
            )

        val savedAsset = assetRepository.save(updatedAsset).awaitSingle()

        // Publish event
        eventPublisher.publishVisibilityUpdated(savedAsset, oldVisibility, newVisibility)

        logger.info { "Visibility updated for asset $assetId" }

        return AssetDto.from(savedAsset)
    }

    /**
     * Bulk update visibility for multiple assets
     */
    suspend fun bulkUpdateVisibility(
        assetIds: List<UUID>,
        visibility: AssetVisibility,
        requestedBy: UUID,
    ): BulkUpdateResult {
        var successCount = 0
        val failedIds = mutableListOf<String>()

        for (assetId in assetIds) {
            try {
                updateVisibility(assetId, requestedBy, visibility)
                successCount++
            } catch (e: Exception) {
                logger.warn(e) { "Failed to update visibility for asset $assetId: ${e.message}" }
                failedIds.add(assetId.toString())
            }
        }

        logger.info { "Bulk visibility update completed: $successCount succeeded, ${failedIds.size} failed" }

        return BulkUpdateResult(
            successCount = successCount,
            failedIds = failedIds,
        )
    }
}

data class BulkUpdateResult(
    val successCount: Int,
    val failedIds: List<String>,
)
