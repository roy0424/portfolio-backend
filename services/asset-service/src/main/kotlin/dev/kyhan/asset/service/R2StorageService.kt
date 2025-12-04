package dev.kyhan.asset.service

import dev.kyhan.asset.config.R2Properties
import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class R2StorageService(
    private val s3AsyncClient: S3AsyncClient,
    private val s3Presigner: S3Presigner,
    private val r2Properties: R2Properties,
    private val urlSigningService: UrlSigningService,
) {
    /**
     * Generate storage path based on visibility
     * Format: {visibility}/{userId}/{assetId}/{originalFileName}
     * This allows CDN URLs to show original filename while preventing collisions via assetId directory
     */
    fun generateStoragePath(
        userId: UUID,
        assetId: UUID,
        originalFileName: String,
        visibility: AssetVisibility,
    ): String {
        // Sanitize filename: remove path separators and dangerous characters
        val sanitizedFileName =
            originalFileName
                .replace(Regex("[/\\\\]"), "_") // Replace path separators
                .take(255) // Limit length to prevent filesystem issues

        // Use assetId as directory to prevent filename collisions
        return "${visibility.name.lowercase()}/$userId/$assetId/$sanitizedFileName"
    }

    /**
     * Generate presigned PUT URL for client-side upload
     */
    fun generatePresignedUploadUrl(
        storagePath: String,
        contentType: String,
        expirationSeconds: Long = 900, // 15 minutes
    ): String {
        try {
            val putObjectRequest =
                PutObjectRequest
                    .builder()
                    .bucket(r2Properties.bucketName)
                    .key(storagePath)
                    .contentType(contentType)
                    .build()

            val presignRequest =
                PutObjectPresignRequest
                    .builder()
                    .signatureDuration(Duration.ofSeconds(expirationSeconds))
                    .putObjectRequest(putObjectRequest)
                    .build()

            val presignedRequest: PresignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest)

            return presignedRequest.url().toString()
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate presigned upload URL: ${e.message}" }
            throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate upload URL")
        }
    }

    /**
     * Generate signed URL for downloading private assets via CDN
     * Uses HMAC-SHA256 signature verified by Cloudflare Worker
     * Storage path already includes original filename: {visibility}/{userId}/{assetId}/{originalFileName}
     * @param storagePath The path to the file in R2
     * @param expirationSeconds How long the URL will be valid (default: 1 hour)
     * @return Signed CDN URL with signature and expiration
     */
    fun generateSignedDownloadUrl(
        storagePath: String,
        expirationSeconds: Long = 3600, // 1 hour
    ): String = urlSigningService.generateSignedUrl(storagePath, expirationSeconds)

    /**
     * Build CDN URL from storage path (for PUBLIC assets only)
     */
    fun buildCdnUrl(storagePath: String): String = "${r2Properties.cdnUrl}/$storagePath"

    /**
     * Copy file from source to destination (for visibility changes)
     */
    suspend fun copyFile(
        sourcePath: String,
        destinationPath: String,
    ) {
        try {
            val copyObjectRequest =
                CopyObjectRequest
                    .builder()
                    .sourceBucket(r2Properties.bucketName)
                    .sourceKey(sourcePath)
                    .destinationBucket(r2Properties.bucketName)
                    .destinationKey(destinationPath)
                    .build()

            s3AsyncClient.copyObject(copyObjectRequest).await()

            logger.info { "File copied successfully: $sourcePath → $destinationPath" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to copy file from $sourcePath to $destinationPath: ${e.message}" }
            throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "File copy failed")
        }
    }

    /**
     * Delete file from R2
     */
    suspend fun deleteFile(storagePath: String) {
        try {
            val deleteRequest =
                DeleteObjectRequest
                    .builder()
                    .bucket(r2Properties.bucketName)
                    .key(storagePath)
                    .build()

            s3AsyncClient.deleteObject(deleteRequest).await()

            logger.info { "File deleted successfully: $storagePath" }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to delete file from R2: ${e.message}" }
            // Don't throw - allow soft delete to proceed even if R2 delete fails
        }
    }

    /**
     * Check if file exists in R2
     */
    suspend fun checkFileExists(storagePath: String): Boolean =
        try {
            val headObjectRequest =
                HeadObjectRequest
                    .builder()
                    .bucket(r2Properties.bucketName)
                    .key(storagePath)
                    .build()

            s3AsyncClient.headObject(headObjectRequest).await()
            true
        } catch (e: NoSuchKeyException) {
            false
        } catch (e: Exception) {
            logger.error(e) { "Failed to check file existence: ${e.message}" }
            false
        }
}
