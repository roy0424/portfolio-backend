package dev.kyhan.auth.controller

import dev.kyhan.auth.dto.AuthResponse
import dev.kyhan.auth.dto.EmailVerificationResponse
import dev.kyhan.auth.dto.RefreshTokenRequest
import dev.kyhan.auth.dto.RegisterEmailRequest
import dev.kyhan.auth.dto.VerifyEmailRequest
import dev.kyhan.auth.service.EmailVerificationService
import dev.kyhan.auth.service.TokenService
import dev.kyhan.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication and token management APIs")
class AuthController(
    private val tokenService: TokenService,
    private val emailVerificationService: EmailVerificationService,
) {
    @Operation(
        summary = "Refresh access token",
        description = "Generate new access and refresh tokens using a valid refresh token",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Tokens refreshed successfully",
        content = [Content(schema = Schema(implementation = AuthResponse::class))],
    )
    @SwaggerApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): Mono<ApiResponse<AuthResponse>> =
        tokenService
            .refreshToken(request.refreshToken)
            .map { ApiResponse.success(it) }

    @Operation(
        summary = "Register email for OAuth user",
        description = "Send verification code to email address for OAuth-authenticated users",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Verification email sent successfully",
        content = [Content(schema = Schema(implementation = EmailVerificationResponse::class))],
    )
    @SwaggerApiResponse(responseCode = "400", description = "Invalid email or email already registered")
    @SwaggerApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    @PostMapping("/email/register")
    fun registerEmail(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
        @Valid @RequestBody request: RegisterEmailRequest,
    ): Mono<ApiResponse<EmailVerificationResponse>> =
        emailVerificationService
            .registerEmail(UUID.fromString(userId), request.email)
            .map {
                ApiResponse.success(
                    EmailVerificationResponse(
                        message = "Verification email sent successfully",
                        email = request.email,
                    ),
                )
            }

    @Operation(
        summary = "Verify email with code",
        description = "Verify email address using the 6-digit code sent to the user's email",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Email verified successfully",
        content = [Content(schema = Schema(implementation = EmailVerificationResponse::class))],
    )
    @SwaggerApiResponse(responseCode = "400", description = "Invalid or expired verification code")
    @SwaggerApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    @PostMapping("/email/verify")
    fun verifyEmail(
        @Parameter(description = "User ID from JWT token", required = true)
        @RequestHeader("X-User-Id")
        userId: String,
        @Valid @RequestBody request: VerifyEmailRequest,
    ): Mono<ApiResponse<EmailVerificationResponse>> =
        emailVerificationService
            .verifyEmail(UUID.fromString(userId), request.code)
            .map {
                ApiResponse.success(
                    EmailVerificationResponse(
                        message = "Email verified successfully",
                        email = null,
                    ),
                )
            }
}
