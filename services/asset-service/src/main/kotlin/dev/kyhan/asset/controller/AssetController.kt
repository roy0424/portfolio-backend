package dev.kyhan.asset.controller

import dev.kyhan.asset.dto.AssetDto
import dev.kyhan.asset.dto.AssetListResponse
import dev.kyhan.asset.dto.DownloadUrlResponse
import dev.kyhan.asset.dto.InitiateUploadRequest
import dev.kyhan.asset.dto.InitiateUploadResponse
import dev.kyhan.asset.service.AssetService
import dev.kyhan.common.dto.ApiResponse
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.mono
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@RestController
@RequestMapping("/assets")
@Tag(name = "Assets", description = "File upload and management APIs")
class AssetController(
    private val assetService: AssetService,
) {
    @PostMapping("/initiate")
    @Operation(
        summary = "Initiate file upload",
        description = "Returns a presigned URL for client-side direct upload to R2. Client should PUT file to this URL.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Upload initiated successfully",
        content = [Content(schema = Schema(implementation = InitiateUploadResponse::class))],
    )
    fun initiateUpload(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
        @Valid @RequestBody request: InitiateUploadRequest,
    ): Mono<ApiResponse<InitiateUploadResponse>> =
        mono {
            val userIdUuid = parseUUID(userId, "userId")
            val siteIdUuid = request.siteId?.let { parseUUID(it, "siteId") }

            val result = assetService.initiateUpload(userIdUuid, siteIdUuid, request)

            ApiResponse.success(result)
        }

    @PostMapping("/{assetId}/complete")
    @Operation(
        summary = "Complete file upload",
        description = "Notify server that client has finished uploading to presigned URL",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Upload completed successfully",
        content = [Content(schema = Schema(implementation = AssetDto::class))],
    )
    fun completeUpload(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
        @Parameter(description = "Asset ID", required = true)
        @PathVariable
        assetId: String,
    ): Mono<ApiResponse<AssetDto>> =
        mono {
            val userIdUuid = parseUUID(userId, "userId")
            val assetIdUuid = parseUUID(assetId, "assetId")

            val result = assetService.completeUpload(assetIdUuid, userIdUuid)

            ApiResponse.success(result)
        }

    @GetMapping("/{assetId}")
    @Operation(
        summary = "Get asset metadata",
        description = "Retrieve metadata for a specific asset",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Asset retrieved successfully",
        content = [Content(schema = Schema(implementation = AssetDto::class))],
    )
    fun getAsset(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
        @Parameter(description = "Asset ID", required = true)
        @PathVariable
        assetId: String,
    ): Mono<ApiResponse<AssetDto>> {
        val userIdUuid = parseUUID(userId, "userId")
        val assetIdUuid = parseUUID(assetId, "assetId")

        return assetService
            .getAsset(assetIdUuid, userIdUuid)
            .map { ApiResponse.success(it) }
    }

    @GetMapping
    @Operation(
        summary = "List user's assets",
        description = "Get paginated list of assets for current user",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Assets retrieved successfully",
        content = [Content(schema = Schema(implementation = AssetListResponse::class))],
    )
    fun listAssets(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "Page size", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int,
    ): Mono<ApiResponse<AssetListResponse>> {
        val userIdUuid = parseUUID(userId, "userId")

        return assetService
            .listAssets(userIdUuid, page, size)
            .map { ApiResponse.success(it) }
    }

    @DeleteMapping("/{assetId}")
    @Operation(
        summary = "Delete asset",
        description = "Soft delete an asset (marks as deleted, file removed from storage)",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Asset deleted successfully",
    )
    fun deleteAsset(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
        @Parameter(description = "Asset ID", required = true)
        @PathVariable
        assetId: String,
    ): Mono<ApiResponse<Nothing>> =
        mono {
            val userIdUuid = parseUUID(userId, "userId")
            val assetIdUuid = parseUUID(assetId, "assetId")

            assetService.deleteAsset(assetIdUuid, userIdUuid)

            ApiResponse<Nothing>(success = true, data = null)
        }

    @GetMapping("/{assetId}/download")
    @Operation(
        summary = "Get download URL",
        description = "Get a URL for downloading the asset (returns CDN URL)",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Download URL generated",
        content = [Content(schema = Schema(implementation = DownloadUrlResponse::class))],
    )
    fun getDownloadUrl(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
        @Parameter(description = "Asset ID", required = true)
        @PathVariable
        assetId: String,
    ): Mono<ApiResponse<DownloadUrlResponse>> =
        mono {
            val userIdUuid = parseUUID(userId, "userId")
            val assetIdUuid = parseUUID(assetId, "assetId")

            val result = assetService.getDownloadUrl(assetIdUuid, userIdUuid)

            ApiResponse.success(result)
        }

    private fun parseUUID(
        value: String,
        fieldName: String,
    ): UUID =
        try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            throw InvalidInputException(ErrorCode.INVALID_INPUT, "Invalid $fieldName format")
        }
}
