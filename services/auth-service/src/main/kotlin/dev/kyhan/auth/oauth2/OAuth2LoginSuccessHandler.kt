package dev.kyhan.auth.oauth2

import dev.kyhan.auth.domain.AuthProvider
import dev.kyhan.auth.domain.UserAccount
import dev.kyhan.auth.domain.UserProfile
import dev.kyhan.auth.repository.UserAccountRepository
import dev.kyhan.auth.repository.UserProfileRepository
import dev.kyhan.common.security.JwtProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class OAuth2LoginSuccessHandler(
    private val userAccountRepository: UserAccountRepository,
    private val userProfileRepository: UserProfileRepository,
    private val jwtProvider: JwtProvider
) : ServerAuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        val oauth2Token = authentication as OAuth2AuthenticationToken
        val oauth2User = oauth2Token.principal as OAuth2User
        val registrationId = oauth2Token.authorizedClientRegistrationId

        val provider = when (registrationId.uppercase()) {
            "GOOGLE" -> AuthProvider.GOOGLE
            "GITHUB" -> AuthProvider.GITHUB
            else -> {
                logger.error { "Unsupported OAuth2 provider: $registrationId" }
                return Mono.error(IllegalArgumentException("Unsupported provider: $registrationId"))
            }
        }

        val providerId = oauth2User.getAttribute<String>("sub")
            ?: oauth2User.getAttribute<Any>("id")?.toString()
            ?: return Mono.error(IllegalStateException("Cannot extract user ID from OAuth2 provider"))

        // OAuth2 제공자로부터 이메일 추출 (없으면 null)
        val email = oauth2User.getAttribute<String>("email")

        val name = oauth2User.getAttribute<String>("name")
        val picture = oauth2User.getAttribute<String>("picture")
            ?: oauth2User.getAttribute<String>("avatar_url")

        return processOAuth2User(provider, providerId, email, name, picture)
            .flatMap { userId ->
                val accessToken = jwtProvider.generateAccessToken(userId, email)
                val refreshToken = jwtProvider.generateRefreshToken(userId, email)

                // 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
                val redirectUrl = UriComponentsBuilder
                    .fromUriString("http://localhost:3000/auth/callback")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .toUriString()

                val response = webFilterExchange.exchange.response
                response.statusCode = org.springframework.http.HttpStatus.FOUND
                response.headers.location = URI.create(redirectUrl)
                response.setComplete()
            }
    }

    private fun processOAuth2User(
        provider: AuthProvider,
        providerId: String,
        email: String?,
        name: String?,
        picture: String?
    ): Mono<UUID> {
        return userAccountRepository.findByProviderAndProviderId(provider, providerId)
            .switchIfEmpty(
                createNewUser(provider, providerId, email, name, picture)
            )
            .map { it.id!! }
    }

    private fun createNewUser(
        provider: AuthProvider,
        providerId: String,
        email: String?,
        name: String?,
        picture: String?
    ): Mono<UserAccount> {
        val userAccount = UserAccount(
            email = email,
            provider = provider,
            providerId = providerId
        )

        return userAccountRepository.save(userAccount)
            .flatMap { savedUser ->
                val userProfile = UserProfile(
                    userId = savedUser.id!!,
                    displayName = name,
                    avatarUrl = picture
                )

                userProfileRepository.save(userProfile)
                    .thenReturn(savedUser)
            }
            .doOnSuccess {
                logger.info { "New user created: ${it.id} via $provider" }
            }
    }
}