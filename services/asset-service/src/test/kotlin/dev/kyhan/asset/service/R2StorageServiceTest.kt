package dev.kyhan.asset.service

import dev.kyhan.asset.config.R2Properties
import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.CopyObjectResponse
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.util.UUID
import java.util.concurrent.CompletableFuture

@DisplayName("R2StorageService 테스트")
class R2StorageServiceTest {
    private lateinit var s3AsyncClient: S3AsyncClient
    private lateinit var s3Presigner: S3Presigner
    private lateinit var r2Properties: R2Properties
    private lateinit var urlSigningService: UrlSigningService
    private lateinit var service: R2StorageService

    private val userId = UUID.randomUUID()
    private val assetId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        s3AsyncClient = mockk(relaxed = true)
        s3Presigner = mockk(relaxed = true)
        r2Properties =
            R2Properties(
                endpoint = "https://test.r2.cloudflarestorage.com",
                accessKey = "test-access-key",
                secretKey = "test-secret-key",
                bucketName = "test-bucket",
                region = "auto",
                cdnUrl = "https://cdn.example.com",
                signingKey = "test-signing-key-32-chars-long",
            )
        urlSigningService = mockk(relaxed = true)
        service = R2StorageService(s3AsyncClient, s3Presigner, r2Properties, urlSigningService)
    }

    @Nested
    @DisplayName("generateStoragePath")
    inner class GenerateStoragePath {
        @Test
        fun `PUBLIC 파일 경로 생성`() {
            val path = service.generateStoragePath(userId, assetId, "photo.jpg", AssetVisibility.PUBLIC)

            assert(path == "public/$userId/$assetId/photo.jpg")
        }

        @Test
        fun `PRIVATE 파일 경로 생성`() {
            val path = service.generateStoragePath(userId, assetId, "document.pdf", AssetVisibility.PRIVATE)

            assert(path == "private/$userId/$assetId/document.pdf")
        }

        @Test
        fun `확장자가 없는 파일`() {
            val path = service.generateStoragePath(userId, assetId, "noextension", AssetVisibility.PUBLIC)

            assert(path == "public/$userId/$assetId/noextension")
        }

        @Test
        fun `여러 점이 있는 파일명`() {
            val path = service.generateStoragePath(userId, assetId, "file.backup.tar.gz", AssetVisibility.PRIVATE)

            assert(path == "private/$userId/$assetId/file.backup.tar.gz")
        }
    }

    @Nested
    @DisplayName("generatePresignedUploadUrl")
    inner class GeneratePresignedUploadUrl {
        @Test
        fun `presigned URL 생성 성공`() {
            val storagePath = "public/$userId/$assetId.jpg"
            val presignedRequest =
                mockk<PresignedPutObjectRequest> {
                    every { url() } returns URI.create("https://r2.example.com/presigned-url").toURL()
                }

            every { s3Presigner.presignPutObject(any<PutObjectPresignRequest>()) } returns presignedRequest

            val url = service.generatePresignedUploadUrl(storagePath, "image/jpeg", 900)

            assert(url == "https://r2.example.com/presigned-url")
            verify(exactly = 1) { s3Presigner.presignPutObject(any<PutObjectPresignRequest>()) }
        }

        @Test
        fun `presigner 실패 시 INTERNAL_SERVER_ERROR`() {
            val storagePath = "public/$userId/$assetId.jpg"

            every { s3Presigner.presignPutObject(any<PutObjectPresignRequest>()) } throws RuntimeException("Presigner error")

            val ex =
                assertThrows<BusinessException> {
                    service.generatePresignedUploadUrl(storagePath, "image/jpeg", 900)
                }

            assert(ex.errorCode == ErrorCode.INTERNAL_SERVER_ERROR)
        }
    }

    @Nested
    @DisplayName("buildCdnUrl")
    inner class BuildCdnUrl {
        @Test
        fun `CDN URL 생성`() {
            val storagePath = "public/$userId/$assetId.jpg"

            val url = service.buildCdnUrl(storagePath)

            assert(url == "https://cdn.example.com/public/$userId/$assetId.jpg")
        }
    }

    @Nested
    @DisplayName("copyFile")
    inner class CopyFile {
        @Test
        fun `파일 복사 성공`() =
            runBlocking {
                val sourcePath = "public/$userId/$assetId.jpg"
                val destPath = "private/$userId/$assetId.jpg"

                val response = mockk<CopyObjectResponse>()
                every { s3AsyncClient.copyObject(any<CopyObjectRequest>()) } returns
                    CompletableFuture.completedFuture(response)

                service.copyFile(sourcePath, destPath)

                verify(exactly = 1) { s3AsyncClient.copyObject(any<CopyObjectRequest>()) }
            }

        @Test
        fun `파일 복사 실패 시 INTERNAL_SERVER_ERROR`() {
            val sourcePath = "public/$userId/$assetId.jpg"
            val destPath = "private/$userId/$assetId.jpg"

            every { s3AsyncClient.copyObject(any<CopyObjectRequest>()) } returns
                CompletableFuture.failedFuture(RuntimeException("Copy failed"))

            val ex =
                assertThrows<BusinessException> {
                    runBlocking {
                        service.copyFile(sourcePath, destPath)
                    }
                }

            assert(ex.errorCode == ErrorCode.INTERNAL_SERVER_ERROR)
        }
    }

    @Nested
    @DisplayName("deleteFile")
    inner class DeleteFile {
        @Test
        fun `파일 삭제 성공`() =
            runBlocking {
                val storagePath = "public/$userId/$assetId.jpg"
                val response = mockk<DeleteObjectResponse>()

                every { s3AsyncClient.deleteObject(any<DeleteObjectRequest>()) } returns
                    CompletableFuture.completedFuture(response)

                service.deleteFile(storagePath)

                verify(exactly = 1) { s3AsyncClient.deleteObject(any<DeleteObjectRequest>()) }
            }

        @Test
        fun `파일 삭제 실패해도 예외 발생 안 함`() =
            runBlocking {
                val storagePath = "public/$userId/$assetId.jpg"

                every { s3AsyncClient.deleteObject(any<DeleteObjectRequest>()) } returns
                    CompletableFuture.failedFuture(RuntimeException("Delete failed"))

                // Should not throw exception
                service.deleteFile(storagePath)

                verify(exactly = 1) { s3AsyncClient.deleteObject(any<DeleteObjectRequest>()) }
            }
    }

    @Nested
    @DisplayName("checkFileExists")
    inner class CheckFileExists {
        @Test
        fun `파일 존재 시 true 반환`() =
            runBlocking {
                val storagePath = "public/$userId/$assetId.jpg"
                val response = mockk<HeadObjectResponse>()

                every { s3AsyncClient.headObject(any<HeadObjectRequest>()) } returns
                    CompletableFuture.completedFuture(response)

                val exists = service.checkFileExists(storagePath)

                assert(exists)
            }

        @Test
        fun `파일 없을 시 false 반환`() =
            runBlocking {
                val storagePath = "public/$userId/$assetId.jpg"

                every { s3AsyncClient.headObject(any<HeadObjectRequest>()) } returns
                    CompletableFuture.failedFuture(NoSuchKeyException.builder().build())

                val exists = service.checkFileExists(storagePath)

                assert(!exists)
            }

        @Test
        fun `기타 에러 시 false 반환`() =
            runBlocking {
                val storagePath = "public/$userId/$assetId.jpg"

                every { s3AsyncClient.headObject(any<HeadObjectRequest>()) } returns
                    CompletableFuture.failedFuture(RuntimeException("S3 error"))

                val exists = service.checkFileExists(storagePath)

                assert(!exists)
            }
    }
}
