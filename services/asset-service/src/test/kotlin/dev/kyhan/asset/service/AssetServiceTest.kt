package dev.kyhan.asset.service

import dev.kyhan.asset.config.AssetProperties
import dev.kyhan.asset.config.R2Properties
import dev.kyhan.asset.domain.Asset
import dev.kyhan.asset.domain.AssetStatus
import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.asset.dto.InitiateUploadRequest
import dev.kyhan.asset.repository.AssetRepository
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import dev.kyhan.common.exception.NotFoundException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

@DisplayName("AssetService 테스트")
class AssetServiceTest {
    private lateinit var assetRepository: AssetRepository
    private lateinit var r2StorageService: R2StorageService
    private lateinit var assetProperties: AssetProperties
    private lateinit var r2Properties: R2Properties
    private lateinit var eventPublisher: AssetEventPublisher
    private lateinit var service: AssetService

    private val userId = UUID.randomUUID()
    private val siteId = UUID.randomUUID()
    private val assetId = UUID.randomUUID()
    private val now = Instant.now()

    private val activeAsset =
        Asset(
            id = assetId,
            userId = userId,
            siteId = siteId,
            fileName = "test-image.jpg",
            originalFileName = "my-photo.jpg",
            contentType = "image/jpeg",
            fileSize = 1024000L,
            storagePath = "public/$userId/$assetId/my-photo.jpg",
            visibility = AssetVisibility.PUBLIC,
            status = AssetStatus.ACTIVE,
            uploadedAt = now,
        )

    @BeforeEach
    fun setUp() {
        assetRepository = mockk(relaxed = true)
        r2StorageService = mockk(relaxed = true)
        assetProperties =
            AssetProperties(
                maxFileSize = 1073741824L, // 1GB
                allowedContentTypes = emptyList(),
            )
        r2Properties =
            R2Properties(
                endpoint = "https://endpoint.r2.cloudflarestorage.com",
                accessKey = "test-access-key",
                secretKey = "test-secret-key",
                bucketName = "test-bucket",
                region = "auto",
                cdnUrl = "https://cdn.example.com",
                signingKey = "test-signing-key",
            )
        eventPublisher = mockk(relaxed = true)
        service = AssetService(assetRepository, r2StorageService, assetProperties, eventPublisher, r2Properties)
    }

    @Nested
    @DisplayName("initiateUpload")
    inner class InitiateUpload {
        @Test
        fun `업로드 시작 성공`() =
            runBlocking {
                val request =
                    InitiateUploadRequest(
                        originalFileName = "test.jpg",
                        contentType = "image/jpeg",
                        fileSize = 1024000L,
                        siteId = siteId.toString(),
                        visibility = AssetVisibility.PRIVATE,
                    )

                val processingAsset = activeAsset.copy(id = null, status = AssetStatus.PROCESSING)
                val savedAsset = processingAsset.copy(id = assetId)

                every { assetRepository.save(any()) } returns Mono.just(savedAsset)
                every { r2StorageService.generateStoragePath(any(), any(), any(), any()) } returns
                    "private/$userId/$assetId.jpg"
                every { r2StorageService.buildCdnUrl(any()) } returns "https://cdn.example.com/private/$userId/$assetId.jpg"
                every { r2StorageService.generatePresignedUploadUrl(any(), any(), any()) } returns
                    "https://r2.example.com/presigned-url"

                val result = service.initiateUpload(userId, siteId, request)

                assert(result.assetId == assetId.toString())
                assert(result.uploadUrl == "https://r2.example.com/presigned-url")
                assert(result.visibility == AssetVisibility.PRIVATE)
                assert(result.expiresAt.isAfter(Instant.now()))

                verify(exactly = 2) { assetRepository.save(any()) }
            }

        @Test
        fun `파일 크기 초과 시 FILE_TOO_LARGE`() {
            val request =
                InitiateUploadRequest(
                    originalFileName = "huge.jpg",
                    contentType = "image/jpeg",
                    fileSize = 2000000000L, // 2GB
                    visibility = AssetVisibility.PUBLIC,
                )

            val ex =
                assertThrows<InvalidInputException> {
                    runBlocking {
                        service.initiateUpload(userId, null, request)
                    }
                }

            assert(ex.errorCode == ErrorCode.FILE_TOO_LARGE)
        }

        @Test
        fun `허용되지 않은 파일 타입 시 INVALID_FILE_TYPE`() {
            assetProperties =
                AssetProperties(
                    maxFileSize = 1073741824L,
                    allowedContentTypes = listOf("image/"),
                )
            service = AssetService(assetRepository, r2StorageService, assetProperties, eventPublisher, r2Properties)

            val request =
                InitiateUploadRequest(
                    originalFileName = "doc.pdf",
                    contentType = "application/pdf",
                    fileSize = 1024000L,
                    visibility = AssetVisibility.PRIVATE,
                )

            val ex =
                assertThrows<InvalidInputException> {
                    runBlocking {
                        service.initiateUpload(userId, null, request)
                    }
                }

            assert(ex.errorCode == ErrorCode.INVALID_FILE_TYPE)
        }
    }

    @Nested
    @DisplayName("completeUpload")
    inner class CompleteUpload {
        @Test
        fun `업로드 완료 성공`() =
            runBlocking {
                val processingAsset = activeAsset.copy(status = AssetStatus.PROCESSING)
                val completedAsset = processingAsset.copy(status = AssetStatus.ACTIVE)

                every { assetRepository.findById(assetId) } returns Mono.just(processingAsset)
                coEvery { r2StorageService.checkFileExists(any()) } returns true
                every { assetRepository.save(any()) } returns Mono.just(completedAsset)

                val result = service.completeUpload(assetId, userId)

                assert(result.id == assetId.toString())
                assert(result.status == AssetStatus.ACTIVE)

                verify(exactly = 1) { assetRepository.findById(assetId) }
                verify(exactly = 1) { assetRepository.save(any()) }
                verify(exactly = 1) { eventPublisher.publishAssetUploaded(any()) }
            }

        @Test
        fun `존재하지 않는 asset이면 ASSET_NOT_FOUND`() {
            every { assetRepository.findById(assetId) } returns Mono.empty()

            val ex =
                assertThrows<NotFoundException> {
                    runBlocking {
                        service.completeUpload(assetId, userId)
                    }
                }

            assert(ex.errorCode == ErrorCode.ASSET_NOT_FOUND)
        }

        @Test
        fun `소유자가 아니면 FORBIDDEN`() {
            val otherUserId = UUID.randomUUID()
            val processingAsset = activeAsset.copy(status = AssetStatus.PROCESSING)

            every { assetRepository.findById(assetId) } returns Mono.just(processingAsset)

            val ex =
                assertThrows<BusinessException> {
                    runBlocking {
                        service.completeUpload(assetId, otherUserId)
                    }
                }

            assert(ex.errorCode == ErrorCode.FORBIDDEN)
        }

        @Test
        fun `PROCESSING 상태가 아니면 INVALID_INPUT`() {
            val alreadyActiveAsset = activeAsset.copy(status = AssetStatus.ACTIVE)

            every { assetRepository.findById(assetId) } returns Mono.just(alreadyActiveAsset)

            val ex =
                assertThrows<BusinessException> {
                    runBlocking {
                        service.completeUpload(assetId, userId)
                    }
                }

            assert(ex.errorCode == ErrorCode.INVALID_INPUT)
        }

        @Test
        fun `R2에 파일이 없으면 ASSET_NOT_FOUND`() {
            val processingAsset = activeAsset.copy(status = AssetStatus.PROCESSING)

            every { assetRepository.findById(assetId) } returns Mono.just(processingAsset)
            coEvery { r2StorageService.checkFileExists(any()) } returns false

            val ex =
                assertThrows<BusinessException> {
                    runBlocking {
                        service.completeUpload(assetId, userId)
                    }
                }

            assert(ex.errorCode == ErrorCode.ASSET_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("getAsset")
    inner class GetAsset {
        @Test
        fun `asset 조회 성공`() {
            every { assetRepository.findActiveById(assetId) } returns Mono.just(activeAsset)

            StepVerifier
                .create(service.getAsset(assetId, userId))
                .expectNextMatches { dto ->
                    dto.id == assetId.toString() &&
                        dto.userId == userId.toString() &&
                        dto.fileName == "test-image.jpg"
                }.verifyComplete()
        }

        @Test
        fun `존재하지 않는 asset이면 ASSET_NOT_FOUND`() {
            every { assetRepository.findActiveById(assetId) } returns Mono.empty()

            StepVerifier
                .create(service.getAsset(assetId, userId))
                .expectErrorSatisfies { error ->
                    assert(error is NotFoundException)
                    val ex = error as NotFoundException
                    assert(ex.errorCode == ErrorCode.ASSET_NOT_FOUND)
                }.verify()
        }

        @Test
        fun `소유자가 아니면 FORBIDDEN`() {
            val otherUserId = UUID.randomUUID()

            every { assetRepository.findActiveById(assetId) } returns Mono.just(activeAsset)

            StepVerifier
                .create(service.getAsset(assetId, otherUserId))
                .expectErrorSatisfies { error ->
                    assert(error is BusinessException)
                    val ex = error as BusinessException
                    assert(ex.errorCode == ErrorCode.FORBIDDEN)
                }.verify()
        }
    }

    @Nested
    @DisplayName("listAssets")
    inner class ListAssets {
        @Test
        fun `asset 목록 조회 성공`() {
            val asset1 = activeAsset.copy(id = UUID.randomUUID())
            val asset2 = activeAsset.copy(id = UUID.randomUUID())

            every { assetRepository.findActiveByUserId(userId, 20, 0) } returns
                Flux.just(asset1, asset2)
            every { assetRepository.countActiveByUserId(userId) } returns Mono.just(2L)

            StepVerifier
                .create(service.listAssets(userId, 0, 20))
                .expectNextMatches { response ->
                    response.page.content.size == 2 &&
                        response.page.totalElements == 2L &&
                        response.page.page == 0 &&
                        response.page.size == 20
                }.verifyComplete()
        }

        @Test
        fun `빈 목록 조회`() {
            every { assetRepository.findActiveByUserId(userId, 20, 0) } returns Flux.empty()
            every { assetRepository.countActiveByUserId(userId) } returns Mono.just(0L)

            StepVerifier
                .create(service.listAssets(userId, 0, 20))
                .expectNextMatches { response ->
                    response.page.content.isEmpty() &&
                        response.page.totalElements == 0L
                }.verifyComplete()
        }
    }

    @Nested
    @DisplayName("deleteAsset")
    inner class DeleteAsset {
        @Test
        fun `asset 삭제 성공`() =
            runBlocking {
                every { assetRepository.findActiveById(assetId) } returns Mono.just(activeAsset)
                every { assetRepository.save(any()) } returns Mono.just(activeAsset.copy(status = AssetStatus.DELETED))
                coEvery { r2StorageService.deleteFile(any()) } returns Unit

                service.deleteAsset(assetId, userId)

                verify(exactly = 1) { assetRepository.findActiveById(assetId) }
                verify(exactly = 1) { assetRepository.save(any()) }
                coVerify(exactly = 1) { r2StorageService.deleteFile(any()) }
                verify(exactly = 1) { eventPublisher.publishAssetDeleted(any()) }
            }

        @Test
        fun `존재하지 않는 asset이면 ASSET_NOT_FOUND`() {
            every { assetRepository.findActiveById(assetId) } returns Mono.empty()

            val ex =
                assertThrows<NotFoundException> {
                    runBlocking {
                        service.deleteAsset(assetId, userId)
                    }
                }

            assert(ex.errorCode == ErrorCode.ASSET_NOT_FOUND)
        }

        @Test
        fun `소유자가 아니면 FORBIDDEN`() {
            val otherUserId = UUID.randomUUID()

            every { assetRepository.findActiveById(assetId) } returns Mono.just(activeAsset)

            val ex =
                assertThrows<BusinessException> {
                    runBlocking {
                        service.deleteAsset(assetId, otherUserId)
                    }
                }

            assert(ex.errorCode == ErrorCode.FORBIDDEN)
        }
    }

    @Nested
    @DisplayName("updateVisibility")
    inner class UpdateVisibility {
        @Test
        fun `visibility 변경 성공 PUBLIC to PRIVATE`() =
            runBlocking {
                val publicAsset = activeAsset.copy(visibility = AssetVisibility.PUBLIC)
                val privateAsset = publicAsset.copy(visibility = AssetVisibility.PRIVATE)

                every { assetRepository.findActiveById(assetId) } returns Mono.just(publicAsset)
                every { r2StorageService.generateStoragePath(any(), any(), any(), AssetVisibility.PRIVATE) } returns
                    "private/$userId/$assetId.jpg"
                coEvery { r2StorageService.copyFile(any(), any()) } returns Unit
                coEvery { r2StorageService.deleteFile(any()) } returns Unit
                every { r2StorageService.buildCdnUrl(any()) } returns "https://cdn.example.com/private/$userId/$assetId.jpg"
                every { assetRepository.save(any()) } returns Mono.just(privateAsset)

                val result = service.updateVisibility(assetId, userId, AssetVisibility.PRIVATE)

                assert(result.visibility == AssetVisibility.PRIVATE)

                coVerify(exactly = 1) { r2StorageService.copyFile(any(), any()) }
                coVerify(exactly = 1) { r2StorageService.deleteFile(any()) }
                verify(exactly = 1) { eventPublisher.publishVisibilityUpdated(any(), AssetVisibility.PUBLIC, AssetVisibility.PRIVATE) }
            }

        @Test
        fun `동일한 visibility면 변경 없이 반환`() =
            runBlocking {
                every { assetRepository.findActiveById(assetId) } returns Mono.just(activeAsset)

                val result = service.updateVisibility(assetId, userId, AssetVisibility.PUBLIC)

                assert(result.visibility == AssetVisibility.PUBLIC)

                coVerify(exactly = 0) { r2StorageService.copyFile(any(), any()) }
                verify(exactly = 0) { eventPublisher.publishVisibilityUpdated(any(), any(), any()) }
            }

        @Test
        fun `ACTIVE 상태가 아니면 INVALID_INPUT`() {
            val processingAsset = activeAsset.copy(status = AssetStatus.PROCESSING)

            every { assetRepository.findActiveById(assetId) } returns Mono.just(processingAsset)

            val ex =
                assertThrows<BusinessException> {
                    runBlocking {
                        service.updateVisibility(assetId, userId, AssetVisibility.PRIVATE)
                    }
                }

            assert(ex.errorCode == ErrorCode.INVALID_INPUT)
        }
    }

    @Nested
    @DisplayName("bulkUpdateVisibility")
    inner class BulkUpdateVisibility {
        @Test
        fun `bulk visibility 업데이트 성공`() =
            runBlocking {
                val assetId1 = UUID.randomUUID()
                val assetId2 = UUID.randomUUID()
                val asset1 = activeAsset.copy(id = assetId1)
                val asset2 = activeAsset.copy(id = assetId2)

                every { assetRepository.findActiveById(assetId1) } returns Mono.just(asset1)
                every { assetRepository.findActiveById(assetId2) } returns Mono.just(asset2)
                every { r2StorageService.generateStoragePath(any(), any(), any(), any()) } returns "private/$userId/test.jpg"
                coEvery { r2StorageService.copyFile(any(), any()) } returns Unit
                coEvery { r2StorageService.deleteFile(any()) } returns Unit
                every { r2StorageService.buildCdnUrl(any()) } returns "https://cdn.example.com/private/test.jpg"
                every { assetRepository.save(any()) } returns Mono.just(asset1.copy(visibility = AssetVisibility.PRIVATE))

                val result = service.bulkUpdateVisibility(listOf(assetId1, assetId2), AssetVisibility.PRIVATE, userId)

                assert(result.successCount == 2)
                assert(result.failedIds.isEmpty())
            }

        @Test
        fun `일부 실패 시 성공 실패 분리`() =
            runBlocking {
                val assetId1 = UUID.randomUUID()
                val assetId2 = UUID.randomUUID()
                val asset1 = activeAsset.copy(id = assetId1)

                every { assetRepository.findActiveById(assetId1) } returns Mono.just(asset1)
                every { assetRepository.findActiveById(assetId2) } returns Mono.empty() // 실패
                every { r2StorageService.generateStoragePath(any(), any(), any(), any()) } returns "private/$userId/test.jpg"
                coEvery { r2StorageService.copyFile(any(), any()) } returns Unit
                coEvery { r2StorageService.deleteFile(any()) } returns Unit
                every { r2StorageService.buildCdnUrl(any()) } returns "https://cdn.example.com/private/test.jpg"
                every { assetRepository.save(any()) } returns Mono.just(asset1.copy(visibility = AssetVisibility.PRIVATE))

                val result = service.bulkUpdateVisibility(listOf(assetId1, assetId2), AssetVisibility.PRIVATE, userId)

                assert(result.successCount == 1)
                assert(result.failedIds.size == 1)
                assert(result.failedIds.contains(assetId2.toString()))
            }
    }
}
