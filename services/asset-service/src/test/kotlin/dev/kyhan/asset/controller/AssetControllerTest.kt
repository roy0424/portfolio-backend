package dev.kyhan.asset.controller

import dev.kyhan.asset.config.GlobalExceptionHandler
import dev.kyhan.asset.domain.AssetStatus
import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.asset.dto.AssetDto
import dev.kyhan.asset.dto.AssetListResponse
import dev.kyhan.asset.dto.DownloadUrlResponse
import dev.kyhan.asset.dto.InitiateUploadRequest
import dev.kyhan.asset.dto.InitiateUploadResponse
import dev.kyhan.asset.service.AssetService
import dev.kyhan.common.dto.PageResponse
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import dev.kyhan.common.exception.NotFoundException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@DisplayName("AssetController 테스트")
class AssetControllerTest {
    private lateinit var assetService: AssetService
    private lateinit var controller: AssetController
    private lateinit var webTestClient: WebTestClient

    private val userId = UUID.randomUUID()
    private val assetId = UUID.randomUUID()
    private val now = Instant.now()

    @BeforeEach
    fun setUp() {
        assetService = mockk(relaxed = true)
        controller = AssetController(assetService)
        webTestClient =
            WebTestClient
                .bindToController(controller)
                .controllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("POST /assets/initiate")
    inner class InitiateUpload {
        @Test
        fun `업로드 시작 성공`() {
            val request =
                InitiateUploadRequest(
                    originalFileName = "test.jpg",
                    contentType = "image/jpeg",
                    fileSize = 1024000L,
                    visibility = AssetVisibility.PRIVATE,
                )

            val response =
                InitiateUploadResponse(
                    assetId = assetId.toString(),
                    uploadUrl = "https://r2.example.com/presigned-url",
                    cdnUrl = null,
                    visibility = AssetVisibility.PRIVATE,
                    expiresAt = now.plusSeconds(900),
                )

            coEvery { assetService.initiateUpload(userId, null, request) } returns response

            webTestClient
                .post()
                .uri("/assets/initiate")
                .header("X-User-Id", userId.toString())
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(true)
                .jsonPath("$.data.assetId")
                .isEqualTo(assetId.toString())
                .jsonPath("$.data.uploadUrl")
                .isEqualTo("https://r2.example.com/presigned-url")
        }

        @Test
        fun `잘못된 userId 형식 시 400`() {
            val request =
                InitiateUploadRequest(
                    originalFileName = "test.jpg",
                    contentType = "image/jpeg",
                    fileSize = 1024000L,
                    visibility = AssetVisibility.PRIVATE,
                )

            webTestClient
                .post()
                .uri("/assets/initiate")
                .header("X-User-Id", "invalid-uuid")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .is4xxClientError
        }
    }

    @Nested
    @DisplayName("POST /assets/{assetId}/complete")
    inner class CompleteUpload {
        @Test
        fun `업로드 완료 성공`() {
            val dto =
                AssetDto(
                    id = assetId.toString(),
                    userId = userId.toString(),
                    siteId = null,
                    fileName = "test.jpg",
                    originalFileName = "my-photo.jpg",
                    contentType = "image/jpeg",
                    fileSize = 1024000L,
                    visibility = AssetVisibility.PRIVATE,
                    status = AssetStatus.ACTIVE,
                    uploadedAt = now,
                )

            coEvery { assetService.completeUpload(assetId, userId) } returns dto

            webTestClient
                .post()
                .uri("/assets/$assetId/complete")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(true)
                .jsonPath("$.data.id")
                .isEqualTo(assetId.toString())
                .jsonPath("$.data.status")
                .isEqualTo("ACTIVE")
        }

        @Test
        fun `존재하지 않는 asset이면 404`() {
            coEvery { assetService.completeUpload(assetId, userId) } throws
                NotFoundException(ErrorCode.ASSET_NOT_FOUND)

            webTestClient
                .post()
                .uri("/assets/$assetId/complete")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus()
                .isNotFound
        }
    }

    @Nested
    @DisplayName("GET /assets/{assetId}")
    inner class GetAsset {
        @Test
        fun `asset 조회 성공`() {
            val dto =
                AssetDto(
                    id = assetId.toString(),
                    userId = userId.toString(),
                    siteId = null,
                    fileName = "test.jpg",
                    originalFileName = "my-photo.jpg",
                    contentType = "image/jpeg",
                    fileSize = 1024000L,
                    visibility = AssetVisibility.PUBLIC,
                    status = AssetStatus.ACTIVE,
                    uploadedAt = now,
                )

            every { assetService.getAsset(assetId, userId) } returns Mono.just(dto)

            webTestClient
                .get()
                .uri("/assets/$assetId")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(true)
                .jsonPath("$.data.id")
                .isEqualTo(assetId.toString())
                .jsonPath("$.data.fileName")
                .isEqualTo("test.jpg")
        }

        @Test
        fun `존재하지 않는 asset이면 404`() {
            every { assetService.getAsset(assetId, userId) } returns
                Mono.error(NotFoundException(ErrorCode.ASSET_NOT_FOUND))

            webTestClient
                .get()
                .uri("/assets/$assetId")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus()
                .isNotFound
        }
    }

    @Nested
    @DisplayName("GET /assets")
    inner class ListAssets {
        @Test
        fun `asset 목록 조회 성공`() {
            val dto1 =
                AssetDto(
                    id = UUID.randomUUID().toString(),
                    userId = userId.toString(),
                    siteId = null,
                    fileName = "test1.jpg",
                    originalFileName = "photo1.jpg",
                    contentType = "image/jpeg",
                    fileSize = 1024000L,
                    visibility = AssetVisibility.PUBLIC,
                    status = AssetStatus.ACTIVE,
                    uploadedAt = now,
                )

            val dto2 =
                AssetDto(
                    id = UUID.randomUUID().toString(),
                    userId = userId.toString(),
                    siteId = null,
                    fileName = "test2.jpg",
                    originalFileName = "photo2.jpg",
                    contentType = "image/jpeg",
                    fileSize = 2048000L,
                    visibility = AssetVisibility.PUBLIC,
                    status = AssetStatus.ACTIVE,
                    uploadedAt = now,
                )

            val response =
                AssetListResponse(
                    page =
                        PageResponse.of(
                            content = listOf(dto1, dto2),
                            page = 0,
                            size = 20,
                            totalElements = 2L,
                        ),
                )

            every { assetService.listAssets(userId, 0, 20) } returns Mono.just(response)

            webTestClient
                .get()
                .uri("/assets?page=0&size=20")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(true)
                .jsonPath("$.data.page.content.length()")
                .isEqualTo(2)
                .jsonPath("$.data.page.totalElements")
                .isEqualTo(2)
        }

        @Test
        fun `빈 목록 조회`() {
            val response =
                AssetListResponse(
                    page =
                        PageResponse.of(
                            content = emptyList(),
                            page = 0,
                            size = 20,
                            totalElements = 0L,
                        ),
                )

            every { assetService.listAssets(userId, 0, 20) } returns Mono.just(response)

            webTestClient
                .get()
                .uri("/assets?page=0&size=20")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(true)
                .jsonPath("$.data.page.content.length()")
                .isEqualTo(0)
                .jsonPath("$.data.page.totalElements")
                .isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("DELETE /assets/{assetId}")
    inner class DeleteAsset {
        @Test
        fun `asset 삭제 성공`() {
            coEvery { assetService.deleteAsset(assetId, userId) } returns Unit

            webTestClient
                .delete()
                .uri("/assets/$assetId")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(true)
        }

        @Test
        fun `존재하지 않는 asset이면 404`() {
            coEvery { assetService.deleteAsset(assetId, userId) } throws
                NotFoundException(ErrorCode.ASSET_NOT_FOUND)

            webTestClient
                .delete()
                .uri("/assets/$assetId")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus()
                .isNotFound
        }
    }

    @Nested
    @DisplayName("GET /assets/{assetId}/download")
    inner class GetDownloadUrl {
        @Test
        fun `다운로드 URL 조회 성공`() {
            val response =
                DownloadUrlResponse(
                    url = "https://cdn.example.com/public/$userId/$assetId.jpg",
                    expiresAt = null,
                )

            coEvery { assetService.getDownloadUrl(assetId, userId) } returns response

            webTestClient
                .get()
                .uri("/assets/$assetId/download")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(true)
                .jsonPath("$.data.url")
                .isEqualTo("https://cdn.example.com/public/$userId/$assetId.jpg")
        }
    }

    @Nested
    @DisplayName("parseUUID")
    inner class ParseUUID {
        @Test
        fun `유효한 UUID 파싱 성공`() {
            val request =
                InitiateUploadRequest(
                    originalFileName = "test.jpg",
                    contentType = "image/jpeg",
                    fileSize = 1024000L,
                    visibility = AssetVisibility.PRIVATE,
                )

            val response =
                InitiateUploadResponse(
                    assetId = assetId.toString(),
                    uploadUrl = "https://r2.example.com/presigned-url",
                    cdnUrl = null,
                    visibility = AssetVisibility.PRIVATE,
                    expiresAt = now.plusSeconds(900),
                )

            coEvery { assetService.initiateUpload(userId, null, request) } returns response

            webTestClient
                .post()
                .uri("/assets/initiate")
                .header("X-User-Id", userId.toString())
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk
        }

        @Test
        fun `잘못된 UUID 형식 시 INVALID_INPUT`() {
            coEvery { assetService.initiateUpload(any(), any(), any()) } throws
                InvalidInputException(ErrorCode.INVALID_INPUT, "Invalid userId format")

            webTestClient
                .post()
                .uri("/assets/initiate")
                .header("X-User-Id", "not-a-uuid")
                .bodyValue(
                    InitiateUploadRequest(
                        originalFileName = "test.jpg",
                        contentType = "image/jpeg",
                        fileSize = 1024000L,
                        visibility = AssetVisibility.PRIVATE,
                    ),
                ).exchange()
                .expectStatus()
                .is4xxClientError
        }
    }
}
