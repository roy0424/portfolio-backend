package dev.kyhan.auth.controller

import dev.kyhan.auth.dto.AuthResponse
import dev.kyhan.auth.dto.EmailVerificationResponse
import dev.kyhan.auth.dto.RefreshTokenRequest
import dev.kyhan.auth.dto.RegisterEmailRequest
import dev.kyhan.auth.dto.VerifyEmailRequest
import dev.kyhan.auth.service.EmailVerificationService
import dev.kyhan.auth.service.TokenService
import dev.kyhan.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/auth")
class AuthController(
    private val tokenService: TokenService,
    private val emailVerificationService: EmailVerificationService,
) {
    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): Mono<ApiResponse<AuthResponse>> =
        tokenService
            .refreshToken(request.refreshToken)
            .map { ApiResponse.success(it) }

    @PostMapping("/email/register")
    fun registerEmail(
        @RequestHeader("X-User-Id") userId: String,
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

    @PostMapping("/email/verify")
    fun verifyEmail(
        @RequestHeader("X-User-Id") userId: String,
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
