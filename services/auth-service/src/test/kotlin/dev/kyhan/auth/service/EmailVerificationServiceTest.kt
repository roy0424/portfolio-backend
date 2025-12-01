package dev.kyhan.auth.service

import dev.kyhan.auth.domain.AuthProvider
import dev.kyhan.auth.domain.EmailVerificationCode
import dev.kyhan.auth.domain.EmailVerificationRateLimit
import dev.kyhan.auth.domain.UserAccount
import dev.kyhan.auth.repository.EmailVerificationCodeRepository
import dev.kyhan.auth.repository.EmailVerificationRateLimitRepository
import dev.kyhan.auth.repository.UserAccountRepository
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ConflictException
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import dev.kyhan.common.exception.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

@DisplayName("EmailVerificationService 테스트")
class EmailVerificationServiceTest {
    private lateinit var userAccountRepository: UserAccountRepository
    private lateinit var codeRepository: EmailVerificationCodeRepository
    private lateinit var rateLimitRepository: EmailVerificationRateLimitRepository
    private lateinit var emailService: EmailService
    private lateinit var service: EmailVerificationService

    private val testUserId = UUID.randomUUID()
    private val testEmail = "test@example.com"
    private val testCode = "123456"

    @BeforeEach
    fun setUp() {
        userAccountRepository = mockk(relaxed = true)
        codeRepository = mockk(relaxed = true)
        rateLimitRepository = mockk(relaxed = true)
        emailService = mockk(relaxed = true)

        service =
            EmailVerificationService(
                userAccountRepository,
                codeRepository,
                rateLimitRepository,
                emailService,
            )
    }

    @Nested
    @DisplayName("registerEmail 테스트")
    inner class RegisterEmailTest {
        @Test
        @DisplayName("성공: 이메일 등록 요청이 정상 처리됨")
        fun registerEmail_Success() {
            // Given
            val userAccount =
                UserAccount(
                    id = testUserId,
                    email = null,
                    provider = AuthProvider.GOOGLE,
                    providerId = "google-123",
                )

            every { rateLimitRepository.findById(testUserId.toString()) } returns Mono.empty()
            every { rateLimitRepository.save(any()) } returns Mono.just(EmailVerificationRateLimit(userId = testUserId.toString()))
            every { userAccountRepository.findById(testUserId) } returns Mono.just(userAccount)
            every { userAccountRepository.existsByEmail(testEmail) } returns Mono.just(false)
            every { codeRepository.findByUserId(testUserId) } returns Mono.empty()
            every { codeRepository.save(any()) } returns
                Mono.just(
                    EmailVerificationCode(
                        code = testCode,
                        userId = testUserId,
                        email = testEmail,
                    ),
                )
            every { emailService.sendVerificationEmail(testEmail, any()) } returns Mono.empty()

            // When & Then
            StepVerifier
                .create(service.registerEmail(testUserId, testEmail))
                .expectNextMatches { it.length == 6 && it.all { char -> char.isDigit() } }
                .verifyComplete()

            verify(exactly = 1) { userAccountRepository.findById(testUserId) }
            verify(exactly = 1) { userAccountRepository.existsByEmail(testEmail) }
            verify(exactly = 1) { codeRepository.save(any()) }
            verify(exactly = 1) { emailService.sendVerificationEmail(testEmail, any()) }
        }

        @Test
        @DisplayName("실패: 잘못된 이메일 형식")
        fun registerEmail_InvalidEmailFormat() {
            // Given
            val invalidEmail = "invalid-email"

            // When & Then
            StepVerifier
                .create(service.registerEmail(testUserId, invalidEmail))
                .expectErrorMatches { error ->
                    error is InvalidInputException &&
                        error.errorCode == ErrorCode.INVALID_EMAIL_FORMAT
                }.verify()
        }

        @Test
        @DisplayName("실패: 이미 이메일이 등록된 사용자")
        fun registerEmail_EmailAlreadyVerified() {
            // Given
            val userAccount =
                UserAccount(
                    id = testUserId,
                    email = "existing@example.com",
                    provider = AuthProvider.GOOGLE,
                    providerId = "google-123",
                )

            every { rateLimitRepository.findById(testUserId.toString()) } returns Mono.empty()
            every { rateLimitRepository.save(any()) } returns Mono.just(EmailVerificationRateLimit(userId = testUserId.toString()))
            every { userAccountRepository.findById(testUserId) } returns Mono.just(userAccount)

            // When & Then
            StepVerifier
                .create(service.registerEmail(testUserId, testEmail))
                .expectErrorMatches { error ->
                    error is BusinessException &&
                        error.errorCode == ErrorCode.EMAIL_ALREADY_VERIFIED
                }.verify()
        }

        @Test
        @DisplayName("실패: 이미 사용 중인 이메일")
        fun registerEmail_EmailAlreadyExists() {
            // Given
            val userAccount =
                UserAccount(
                    id = testUserId,
                    email = null,
                    provider = AuthProvider.GOOGLE,
                    providerId = "google-123",
                )

            every { rateLimitRepository.findById(testUserId.toString()) } returns Mono.empty()
            every { rateLimitRepository.save(any()) } returns Mono.just(EmailVerificationRateLimit(userId = testUserId.toString()))
            every { userAccountRepository.findById(testUserId) } returns Mono.just(userAccount)
            every { userAccountRepository.existsByEmail(testEmail) } returns Mono.just(true)

            // When & Then
            StepVerifier
                .create(service.registerEmail(testUserId, testEmail))
                .expectErrorMatches { error ->
                    error is ConflictException &&
                        error.errorCode == ErrorCode.EMAIL_ALREADY_EXISTS
                }.verify()
        }

        @Test
        @DisplayName("실패: Rate limit - 쿨다운 기간")
        fun registerEmail_RateLimitCooldown() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId.toString(),
                    requestCount = 1,
                    lastRequestTime = Instant.now().minusSeconds(30), // 30초 전 요청
                )

            every { rateLimitRepository.findById(testUserId.toString()) } returns Mono.just(rateLimit)

            // When & Then
            StepVerifier
                .create(service.registerEmail(testUserId, testEmail))
                .expectErrorMatches { error ->
                    error is BusinessException &&
                        error.errorCode == ErrorCode.VERIFICATION_COOLDOWN
                }.verify()
        }

        @Test
        @DisplayName("실패: Rate limit - 시간당 최대 요청 횟수 초과")
        fun registerEmail_RateLimitMaxRequests() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId.toString(),
                    requestCount = 5,
                    lastRequestTime = Instant.now().minusSeconds(120), // 2분 전
                )

            every { rateLimitRepository.findById(testUserId.toString()) } returns Mono.just(rateLimit)

            // When & Then
            StepVerifier
                .create(service.registerEmail(testUserId, testEmail))
                .expectErrorMatches { error ->
                    error is BusinessException &&
                        error.errorCode == ErrorCode.TOO_MANY_REQUESTS
                }.verify()
        }
    }

    @Nested
    @DisplayName("verifyEmail 테스트")
    inner class VerifyEmailTest {
        @Test
        @DisplayName("성공: 이메일 인증 완료")
        fun verifyEmail_Success() {
            // Given
            val verificationCode =
                EmailVerificationCode(
                    code = testCode,
                    userId = testUserId,
                    email = testEmail,
                )

            val userAccount =
                UserAccount(
                    id = testUserId,
                    email = null,
                    provider = AuthProvider.GOOGLE,
                    providerId = "google-123",
                )

            val updatedAccount = userAccount.copy(email = testEmail, updatedAt = Instant.now())

            every { codeRepository.findById(testCode) } returns Mono.just(verificationCode)
            every { userAccountRepository.findById(testUserId) } returns Mono.just(userAccount)
            every { userAccountRepository.existsByEmail(testEmail) } returns Mono.just(false)
            every { userAccountRepository.save(any()) } returns Mono.just(updatedAccount)
            every { codeRepository.deleteById(testCode) } returns Mono.just(true)
            every { codeRepository.deleteByUserId(testUserId) } returns Mono.empty()
            every { rateLimitRepository.deleteById(testUserId) } returns Mono.just(true)

            // When & Then
            StepVerifier
                .create(service.verifyEmail(testUserId, testCode))
                .expectNext(Unit)
                .verifyComplete()

            verify(exactly = 1) { codeRepository.findById(testCode) }
            verify(exactly = 1) { userAccountRepository.save(any()) }
            verify(exactly = 1) { codeRepository.deleteById(testCode) }
        }

        @Test
        @DisplayName("실패: 잘못된 인증 코드")
        fun verifyEmail_InvalidCode() {
            // Given
            every { codeRepository.findById(testCode) } returns Mono.empty()

            // When & Then
            StepVerifier
                .create(service.verifyEmail(testUserId, testCode))
                .expectErrorMatches { error ->
                    error is BusinessException &&
                        error.errorCode == ErrorCode.INVALID_VERIFICATION_CODE
                }.verify()
        }

        @Test
        @DisplayName("실패: 다른 사용자의 인증 코드")
        fun verifyEmail_WrongUser() {
            // Given
            val otherUserId = UUID.randomUUID()
            val verificationCode =
                EmailVerificationCode(
                    code = testCode,
                    userId = otherUserId,
                    email = testEmail,
                )

            every { codeRepository.findById(testCode) } returns Mono.just(verificationCode)

            // When & Then
            StepVerifier
                .create(service.verifyEmail(testUserId, testCode))
                .expectErrorMatches { error ->
                    error is BusinessException &&
                        error.errorCode == ErrorCode.INVALID_VERIFICATION_CODE
                }.verify()
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없음")
        fun verifyEmail_UserNotFound() {
            // Given
            val verificationCode =
                EmailVerificationCode(
                    code = testCode,
                    userId = testUserId,
                    email = testEmail,
                )

            every { codeRepository.findById(testCode) } returns Mono.just(verificationCode)
            every { userAccountRepository.findById(testUserId) } returns Mono.empty()

            // When & Then
            StepVerifier
                .create(service.verifyEmail(testUserId, testCode))
                .expectErrorMatches { error ->
                    error is NotFoundException &&
                        error.errorCode == ErrorCode.NOT_FOUND
                }.verify()
        }

        @Test
        @DisplayName("실패: 인증 중 다른 사용자가 이메일 등록")
        fun verifyEmail_EmailTakenDuringVerification() {
            // Given
            val verificationCode =
                EmailVerificationCode(
                    code = testCode,
                    userId = testUserId,
                    email = testEmail,
                )

            val userAccount =
                UserAccount(
                    id = testUserId,
                    email = null,
                    provider = AuthProvider.GOOGLE,
                    providerId = "google-123",
                )

            every { codeRepository.findById(testCode) } returns Mono.just(verificationCode)
            every { userAccountRepository.findById(testUserId) } returns Mono.just(userAccount)
            every { userAccountRepository.existsByEmail(testEmail) } returns Mono.just(true)

            // When & Then
            StepVerifier
                .create(service.verifyEmail(testUserId, testCode))
                .expectErrorMatches { error ->
                    error is ConflictException &&
                        error.errorCode == ErrorCode.EMAIL_ALREADY_EXISTS
                }.verify()
        }
    }

    @Nested
    @DisplayName("Rate Limiting 테스트")
    inner class RateLimitingTest {
        @Test
        @DisplayName("성공: 첫 번째 요청")
        fun rateLimit_FirstRequest() {
            // Given
            val userAccount =
                UserAccount(
                    id = testUserId,
                    email = null,
                    provider = AuthProvider.GOOGLE,
                    providerId = "google-123",
                )

            every { rateLimitRepository.findById(testUserId.toString()) } returns Mono.empty()
            every { rateLimitRepository.save(any()) } returns Mono.just(EmailVerificationRateLimit(userId = testUserId.toString()))
            every { userAccountRepository.findById(testUserId) } returns Mono.just(userAccount)
            every { userAccountRepository.existsByEmail(testEmail) } returns Mono.just(false)
            every { codeRepository.findByUserId(testUserId) } returns Mono.empty()
            every { codeRepository.save(any()) } returns
                Mono.just(
                    EmailVerificationCode(
                        code = testCode,
                        userId = testUserId,
                        email = testEmail,
                    ),
                )
            every { emailService.sendVerificationEmail(testEmail, any()) } returns Mono.empty()

            // When & Then
            StepVerifier
                .create(service.registerEmail(testUserId, testEmail))
                .expectNextCount(1)
                .verifyComplete()

            verify(exactly = 1) { rateLimitRepository.save(any()) }
        }

        @Test
        @DisplayName("성공: 쿨다운 이후 두 번째 요청")
        fun rateLimit_SecondRequestAfterCooldown() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId.toString(),
                    requestCount = 1,
                    lastRequestTime = Instant.now().minusSeconds(120), // 2분 전
                )

            val userAccount =
                UserAccount(
                    id = testUserId,
                    email = null,
                    provider = AuthProvider.GOOGLE,
                    providerId = "google-123",
                )

            every { rateLimitRepository.findById(testUserId.toString()) } returns Mono.just(rateLimit)
            every { rateLimitRepository.save(any()) } returns Mono.just(rateLimit.incrementCount())
            every { userAccountRepository.findById(testUserId) } returns Mono.just(userAccount)
            every { userAccountRepository.existsByEmail(testEmail) } returns Mono.just(false)
            every { codeRepository.findByUserId(testUserId) } returns Mono.empty()
            every { codeRepository.save(any()) } returns
                Mono.just(
                    EmailVerificationCode(
                        code = testCode,
                        userId = testUserId,
                        email = testEmail,
                    ),
                )
            every { emailService.sendVerificationEmail(testEmail, any()) } returns Mono.empty()

            // When & Then
            StepVerifier
                .create(service.registerEmail(testUserId, testEmail))
                .expectNextCount(1)
                .verifyComplete()
        }
    }
}
