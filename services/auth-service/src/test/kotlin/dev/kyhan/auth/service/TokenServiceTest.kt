package dev.kyhan.auth.service

import dev.kyhan.auth.domain.AuthProvider
import dev.kyhan.auth.domain.UserAccount
import dev.kyhan.auth.domain.UserProfile
import dev.kyhan.auth.repository.UserAccountRepository
import dev.kyhan.auth.repository.UserProfileRepository
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.UnauthorizedException
import dev.kyhan.common.security.JwtProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID

@DisplayName("TokenService 테스트")
class TokenServiceTest {
    private lateinit var userAccountRepository: UserAccountRepository
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var jwtProvider: JwtProvider
    private lateinit var service: TokenService

    private val testUserId = UUID.randomUUID()
    private val testEmail = "test@example.com"
    private val testRefreshToken = "valid.refresh.token"

    @BeforeEach
    fun setUp() {
        userAccountRepository = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        jwtProvider = mockk(relaxed = true)

        service =
            TokenService(
                userAccountRepository,
                userProfileRepository,
                jwtProvider,
            )
    }

    @Nested
    @DisplayName("refreshToken 테스트")
    inner class RefreshTokenTest {
        @Test
        @DisplayName("성공: 프로필이 있는 사용자의 토큰 갱신")
        fun refreshToken_WithProfile_Success() {
            // Given
            val userAccount =
                UserAccount(
                    id = testUserId,
                    email = testEmail,
                    provider = AuthProvider.GOOGLE,
                    providerId = "google-123",
                )

            val userProfile =
                UserProfile(
                    id = UUID.randomUUID(),
                    userId = testUserId,
                    displayName = "Test User",
                    avatarUrl = "https://example.com/avatar.jpg",
                    bio = "Test bio",
                    website = "https://example.com",
                )

            every { jwtProvider.getUserIdFromToken(testRefreshToken) } returns testUserId
            every { jwtProvider.getEmailFromToken(testRefreshToken) } returns testEmail
            every { jwtProvider.getTokenType(testRefreshToken) } returns JwtProvider.TokenType.REFRESH
            every { userAccountRepository.findById(testUserId) } returns Mono.just(userAccount)
            every { userProfileRepository.findActiveByUserId(testUserId) } returns Mono.just(userProfile)
            every { jwtProvider.generateAccessToken(testUserId, testEmail) } returns "new.access.token"
            every { jwtProvider.generateRefreshToken(testUserId, testEmail) } returns "new.refresh.token"

            // When & Then
            StepVerifier
                .create(service.refreshToken(testRefreshToken))
                .expectNextMatches { response ->
                    response.userId == testUserId.toString() &&
                        response.email == testEmail &&
                        response.accessToken == "new.access.token" &&
                        response.refreshToken == "new.refresh.token" &&
                        response.profile?.displayName == "Test User"
                }.verifyComplete()

            verify(exactly = 1) { userAccountRepository.findById(testUserId) }
            verify(exactly = 1) { userProfileRepository.findActiveByUserId(testUserId) }
        }

        @Test
        @DisplayName("성공: 프로필이 없는 사용자의 토큰 갱신")
        fun refreshToken_WithoutProfile_Success() {
            // Given
            val userAccount =
                UserAccount(
                    id = testUserId,
                    email = testEmail,
                    provider = AuthProvider.GOOGLE,
                    providerId = "google-123",
                )

            every { jwtProvider.getUserIdFromToken(testRefreshToken) } returns testUserId
            every { jwtProvider.getEmailFromToken(testRefreshToken) } returns testEmail
            every { jwtProvider.getTokenType(testRefreshToken) } returns JwtProvider.TokenType.REFRESH
            every { userAccountRepository.findById(testUserId) } returns Mono.just(userAccount)
            every { userProfileRepository.findActiveByUserId(testUserId) } returns Mono.empty()
            every { jwtProvider.generateAccessToken(testUserId, testEmail) } returns "new.access.token"
            every { jwtProvider.generateRefreshToken(testUserId, testEmail) } returns "new.refresh.token"

            // When & Then
            StepVerifier
                .create(service.refreshToken(testRefreshToken))
                .expectNextMatches { response ->
                    response.userId == testUserId.toString() &&
                        response.email == testEmail &&
                        response.accessToken == "new.access.token" &&
                        response.refreshToken == "new.refresh.token" &&
                        response.profile == null
                }.verifyComplete()

            verify(exactly = 1) { userAccountRepository.findById(testUserId) }
            verify(exactly = 1) { userProfileRepository.findActiveByUserId(testUserId) }
        }

        @Test
        @DisplayName("실패: Access Token을 사용한 경우")
        fun refreshToken_WrongTokenType() {
            // Given
            every { jwtProvider.getUserIdFromToken(testRefreshToken) } returns testUserId
            every { jwtProvider.getEmailFromToken(testRefreshToken) } returns testEmail
            every { jwtProvider.getTokenType(testRefreshToken) } returns JwtProvider.TokenType.ACCESS

            // When & Then
            StepVerifier
                .create(service.refreshToken(testRefreshToken))
                .expectErrorMatches { error ->
                    error is UnauthorizedException &&
                        error.errorCode == ErrorCode.INVALID_TOKEN
                }.verify()
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없음")
        fun refreshToken_UserNotFound() {
            // Given
            every { jwtProvider.getUserIdFromToken(testRefreshToken) } returns testUserId
            every { jwtProvider.getEmailFromToken(testRefreshToken) } returns testEmail
            every { jwtProvider.getTokenType(testRefreshToken) } returns JwtProvider.TokenType.REFRESH
            every { userAccountRepository.findById(testUserId) } returns Mono.empty()

            // When & Then
            StepVerifier
                .create(service.refreshToken(testRefreshToken))
                .expectErrorMatches { error ->
                    error is UnauthorizedException &&
                        error.errorCode == ErrorCode.INVALID_TOKEN
                }.verify()
        }

        @Test
        @DisplayName("실패: 유효하지 않은 토큰")
        fun refreshToken_InvalidToken() {
            // Given
            every { jwtProvider.getUserIdFromToken(testRefreshToken) } throws IllegalArgumentException("Invalid token")

            // When & Then
            StepVerifier
                .create(service.refreshToken(testRefreshToken))
                .expectErrorMatches { error ->
                    error is UnauthorizedException &&
                        error.errorCode == ErrorCode.INVALID_TOKEN
                }.verify()
        }
    }
}
