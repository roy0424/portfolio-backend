package dev.kyhan.auth.controller

import dev.kyhan.auth.dto.*
import dev.kyhan.auth.service.EmailVerificationService
import dev.kyhan.auth.service.TokenService
import dev.kyhan.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/auth")
class AuthController(
    private val tokenService: TokenService,
    private val emailVerificationService: EmailVerificationService
) {
    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): Mono<ApiResponse<AuthResponse>> {
        return tokenService.refreshToken(request.refreshToken)
            .map { ApiResponse.success(it) }
    }

    @PostMapping("/email/register")
    fun registerEmail(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: RegisterEmailRequest
    ): Mono<ApiResponse<EmailVerificationResponse>> {
        return emailVerificationService.registerEmail(UUID.fromString(userId), request.email)
            .map {
                ApiResponse.success(
                    EmailVerificationResponse(
                        message = "Verification email sent successfully",
                        email = request.email
                    )
                )
            }
    }

    @PostMapping("/email/verify")
    fun verifyEmail(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: VerifyEmailRequest
    ): Mono<ApiResponse<EmailVerificationResponse>> {
        return emailVerificationService.verifyEmail(UUID.fromString(userId), request.code)
            .map {
                ApiResponse.success(
                    EmailVerificationResponse(
                        message = "Email verified successfully",
                        email = null
                    )
                )
            }
    }

    @PostMapping("/email/resend")
    fun resendVerificationEmail(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: ResendVerificationRequest
    ): Mono<ApiResponse<EmailVerificationResponse>> {
        return emailVerificationService.resendVerificationEmail(UUID.fromString(userId), request.email)
            .map {
                ApiResponse.success(
                    EmailVerificationResponse(
                        message = "Verification email resent successfully",
                        email = request.email
                    )
                )
            }
    }
}