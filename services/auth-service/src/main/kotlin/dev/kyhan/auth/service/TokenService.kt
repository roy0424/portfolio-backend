package dev.kyhan.auth.service

import dev.kyhan.auth.dto.AuthResponse
import dev.kyhan.auth.dto.UserProfileDto
import dev.kyhan.auth.repository.UserAccountRepository
import dev.kyhan.auth.repository.UserProfileRepository
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.UnauthorizedException
import dev.kyhan.common.security.JwtProvider
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TokenService(
    private val userAccountRepository: UserAccountRepository,
    private val userProfileRepository: UserProfileRepository,
    private val jwtProvider: JwtProvider,
) {
    fun refreshToken(refreshToken: String): Mono<AuthResponse> {
        return try {
            val userId = jwtProvider.getUserIdFromToken(refreshToken)
            val email = jwtProvider.getEmailFromToken(refreshToken)
            val tokenType = jwtProvider.getTokenType(refreshToken)

            if (tokenType != JwtProvider.TokenType.REFRESH) {
                return Mono.error(UnauthorizedException(ErrorCode.INVALID_TOKEN, "Not a refresh token"))
            }

            userAccountRepository
                .findById(userId)
                .switchIfEmpty(Mono.error(UnauthorizedException(ErrorCode.INVALID_TOKEN)))
                .flatMap { userAccount ->
                    userProfileRepository
                        .findByUserId(userAccount.id!!)
                        .map { profile ->
                            AuthResponse(
                                userId = userAccount.id.toString(),
                                email = userAccount.email,
                                accessToken = jwtProvider.generateAccessToken(userAccount.id, userAccount.email),
                                refreshToken = jwtProvider.generateRefreshToken(userAccount.id, userAccount.email),
                                profile =
                                    UserProfileDto(
                                        displayName = profile.displayName,
                                        avatarUrl = profile.avatarUrl,
                                        bio = profile.bio,
                                        location = profile.location,
                                        website = profile.website,
                                    ),
                            )
                        }.switchIfEmpty(
                            Mono.just(
                                AuthResponse(
                                    userId = userAccount.id.toString(),
                                    email = userAccount.email,
                                    accessToken = jwtProvider.generateAccessToken(userAccount.id, userAccount.email),
                                    refreshToken = jwtProvider.generateRefreshToken(userAccount.id, userAccount.email),
                                    profile = null,
                                ),
                            ),
                        )
                }
        } catch (e: Exception) {
            Mono.error(UnauthorizedException(ErrorCode.INVALID_TOKEN))
        }
    }
}
