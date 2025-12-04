package dev.kyhan.asset.grpc

import dev.kyhan.asset.config.R2Properties
import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.asset.repository.AssetRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.reactor.awaitSingleOrNull
import net.devh.boot.grpc.server.service.GrpcService
import java.util.UUID

private val logger = KotlinLogging.logger {}

@GrpcService
class AssetGrpcService(
    private val assetRepository: AssetRepository,
    private val r2Properties: R2Properties,
) : AssetServiceGrpcKt.AssetServiceCoroutineImplBase() {
    override suspend fun getAsset(request: GetAssetRequest): GetAssetResponse {
        try {
            val assetId = UUID.fromString(request.assetId)
            val userId = UUID.fromString(request.userId)

            val asset =
                assetRepository.findActiveById(assetId).awaitSingleOrNull()
                    ?: throw StatusException(Status.NOT_FOUND.withDescription("Asset not found"))

            // Verify ownership
            if (asset.userId != userId) {
                throw StatusException(Status.PERMISSION_DENIED.withDescription("Access denied"))
            }

            logger.info { "gRPC GetAsset: assetId=$assetId, userId=$userId" }

            // Build CDN URL dynamically (only for PUBLIC assets)
            val cdnUrl =
                if (asset.visibility == AssetVisibility.PUBLIC) {
                    "${r2Properties.cdnUrl}/${asset.storagePath}"
                } else {
                    "" // PRIVATE assets don't expose CDN URL via gRPC
                }

            return GetAssetResponse
                .newBuilder()
                .setAssetId(asset.id.toString())
                .setUserId(asset.userId.toString())
                .setSiteId(asset.siteId?.toString() ?: "")
                .setFileName(asset.fileName)
                .setOriginalFileName(asset.originalFileName)
                .setContentType(asset.contentType)
                .setFileSize(asset.fileSize)
                .setCdnUrl(cdnUrl)
                .setVisibility(asset.visibility.name)
                .setStatus(asset.status.name)
                .setUploadedAt(asset.uploadedAt.toEpochMilli())
                .build()
        } catch (e: StatusException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid UUID format"))
        } catch (e: Exception) {
            logger.error(e) { "gRPC GetAsset error: ${e.message}" }
            throw StatusException(Status.INTERNAL.withDescription("Internal server error"))
        }
    }

    override suspend fun verifyAssetOwnership(request: VerifyAssetOwnershipRequest): VerifyAssetOwnershipResponse {
        try {
            val assetId = UUID.fromString(request.assetId)
            val userId = UUID.fromString(request.userId)

            val asset = assetRepository.findActiveById(assetId).awaitSingleOrNull()

            if (asset == null) {
                logger.info { "gRPC VerifyAssetOwnership: asset not found - assetId=$assetId" }
                return VerifyAssetOwnershipResponse
                    .newBuilder()
                    .setExists(false)
                    .setIsOwner(false)
                    .setCdnUrl("")
                    .build()
            }

            val isOwner = asset.userId == userId

            logger.info { "gRPC VerifyAssetOwnership: assetId=$assetId, userId=$userId, isOwner=$isOwner" }

            // Build CDN URL dynamically (only for PUBLIC assets and if owner)
            val cdnUrl =
                if (isOwner && asset.visibility == AssetVisibility.PUBLIC) {
                    "${r2Properties.cdnUrl}/${asset.storagePath}"
                } else {
                    ""
                }

            return VerifyAssetOwnershipResponse
                .newBuilder()
                .setExists(true)
                .setIsOwner(isOwner)
                .setCdnUrl(cdnUrl)
                .build()
        } catch (e: IllegalArgumentException) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid UUID format"))
        } catch (e: Exception) {
            logger.error(e) { "gRPC VerifyAssetOwnership error: ${e.message}" }
            throw StatusException(Status.INTERNAL.withDescription("Internal server error"))
        }
    }
}
