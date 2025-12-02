package dev.kyhan.auth.controller

import dev.kyhan.auth.dto.UpdateUserProfileRequest
import dev.kyhan.auth.dto.UserProfileDto
import dev.kyhan.auth.service.UserProfileService
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@RestController
@RequestMapping("/user-profile")
@Tag(name = "User Profile", description = "User profile CRUD APIs")
class UserProfileController(
    private val userProfileService: UserProfileService,
) {
    @GetMapping
    @Operation(
        summary = "Get current user's profile",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Profile retrieved successfully",
        content = [Content(schema = Schema(implementation = UserProfileDto::class))],
    )
    fun getMyProfile(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
    ): Mono<ApiResponse<UserProfileDto>> =
        userProfileService
            .getMyProfile(parseUserId(userId))
            .map { ApiResponse.success(it) }

    @GetMapping("/{userId}")
    @Operation(
        summary = "Get public profile by user ID",
        description = "Public endpoint for fetching a user's profile by their ID",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Profile retrieved successfully",
        content = [Content(schema = Schema(implementation = UserProfileDto::class))],
    )
    fun getProfileByUserId(
        @PathVariable
        userId: String,
    ): Mono<ApiResponse<UserProfileDto>> =
        userProfileService
            .getProfileByUserId(parseUserId(userId))
            .map { ApiResponse.success(it) }

    @PutMapping
    @Operation(
        summary = "Update current user's profile",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Profile updated successfully",
        content = [Content(schema = Schema(implementation = UserProfileDto::class))],
    )
    fun updateProfile(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
        @Valid @RequestBody
        request: UpdateUserProfileRequest,
    ): Mono<ApiResponse<UserProfileDto>> =
        userProfileService
            .updateProfile(parseUserId(userId), request)
            .map { ApiResponse.success(it) }

    private fun parseUserId(userId: String): UUID =
        try {
            UUID.fromString(userId)
        } catch (ex: IllegalArgumentException) {
            throw InvalidInputException(ErrorCode.INVALID_INPUT, "Invalid userId format")
        }
}
