package dev.kyhan.auth.controller

import dev.kyhan.auth.dto.AuthResponse
import dev.kyhan.auth.dto.RefreshTokenRequest
import dev.kyhan.auth.service.TokenService
import dev.kyhan.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/auth")
class AuthController(
    private val tokenService: TokenService
) {
    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): Mono<ApiResponse<AuthResponse>> {
        return tokenService.refreshToken(request.refreshToken)
            .map { ApiResponse.success(it) }
    }
}