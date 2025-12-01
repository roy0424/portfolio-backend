package dev.kyhan.auth.service

import dev.kyhan.auth.domain.EmailVerificationCode
import dev.kyhan.auth.domain.EmailVerificationRateLimit
import dev.kyhan.auth.repository.EmailVerificationCodeRepository
import dev.kyhan.auth.repository.EmailVerificationRateLimitRepository
import dev.kyhan.auth.repository.UserAccountRepository
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ConflictException
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import dev.kyhan.common.exception.NotFoundException
import dev.kyhan.common.util.ValidationUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class EmailVerificationService(
    private val userAccountRepository: UserAccountRepository,
    private val codeRepository: EmailVerificationCodeRepository,
    private val rateLimitRepository: EmailVerificationRateLimitRepository,
    private val emailService: EmailService,
) {
    fun registerEmail(
        userId: UUID,
        email: String,
    ): Mono<String> {
        // 1. 이메일 형식 검증
        if (!ValidationUtils.isValidEmail(email)) {
            return Mono.error(InvalidInputException(ErrorCode.INVALID_EMAIL_FORMAT))
        }

        // 2. rate limit 체크
        return checkAndUpdateRateLimit(userId)
            .then(
                userAccountRepository
                    .findById(userId)
                    .switchIfEmpty(Mono.error(NotFoundException(ErrorCode.NOT_FOUND, "User not found")))
                    .flatMap { userAccount ->
                        // 3. 이미 이메일이 확정된 유저인지(=이미 인증 완료 상태인지) 체크
                        if (userAccount.email != null) {
                            return@flatMap Mono.error<String>(
                                BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED),
                            )
                        }

                        // 4. 이메일 중복 체크
                        userAccountRepository
                            .existsByEmail(email)
                            .flatMap { exists ->
                                if (exists) {
                                    Mono.error(ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS))
                                } else {
                                    // 5. 기존 인증 코드가 있다면 삭제 후 새 코드 생성(=재발송)
                                    codeRepository
                                        .findByUserId(userId)
                                        .flatMap { existingCode ->
                                            codeRepository.deleteById(existingCode.code)
                                        }.then(
                                            Mono.defer {
                                                val code = generateCode()
                                                val verificationCode =
                                                    EmailVerificationCode(
                                                        code = code,
                                                        userId = userId,
                                                        email = email,
                                                    )

                                                codeRepository
                                                    .save(verificationCode)
                                                    .flatMap {
                                                        emailService
                                                            .sendVerificationEmail(email, code)
                                                            .thenReturn(code)
                                                    }.doOnSuccess {
                                                        logger.info {
                                                            "Verification email requested for user: $userId, email: $email"
                                                        }
                                                    }
                                            },
                                        )
                                }
                            }
                    },
            )
    }

    fun verifyEmail(
        userId: UUID,
        code: String,
    ): Mono<Unit> {
        return codeRepository
            .findById(code)
            .switchIfEmpty(Mono.error(BusinessException(ErrorCode.INVALID_VERIFICATION_CODE)))
            .flatMap { verificationCode ->
                // Verify that the code belongs to the requesting user
                if (verificationCode.userId != userId) {
                    return@flatMap Mono.error<Unit>(
                        BusinessException(ErrorCode.INVALID_VERIFICATION_CODE),
                    )
                }

                // Update user email
                userAccountRepository
                    .findById(verificationCode.userId)
                    .switchIfEmpty(Mono.error(NotFoundException(ErrorCode.NOT_FOUND, "User not found")))
                    .flatMap { userAccount ->
                        // Check if email is still available
                        userAccountRepository
                            .existsByEmail(verificationCode.email)
                            .flatMap { exists ->
                                if (exists && userAccount.email != verificationCode.email) {
                                    // Email was taken by another user during verification
                                    Mono.error(ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS))
                                } else {
                                    val updatedAccount =
                                        userAccount.copy(
                                            email = verificationCode.email,
                                            updatedAt = Instant.now(),
                                        )

                                    userAccountRepository
                                        .save(updatedAccount)
                                        .flatMap {
                                            Mono
                                                .`when`(
                                                    codeRepository.deleteById(code),
                                                    codeRepository.deleteByUserId(userId),
                                                    rateLimitRepository.deleteById(userId),
                                                ).doOnSuccess {
                                                    logger.info { "Email verified successfully for user: ${verificationCode.userId}" }
                                                }.thenReturn(Unit)
                                        }
                                }
                            }
                    }
            }
    }

    private fun checkAndUpdateRateLimit(userId: UUID): Mono<Unit> {
        val userIdStr = userId.toString()

        return rateLimitRepository
            .findById(userIdStr)
            .flatMap { rateLimit ->
                // 기존 rate limit이 있으면 체크
                when {
                    !rateLimit.canRequest() -> {
                        val now = Instant.now()
                        val secondsSinceLastRequest = now.epochSecond - rateLimit.lastRequestTime.epochSecond

                        if (secondsSinceLastRequest < EmailVerificationRateLimit.COOLDOWN_SECONDS) {
                            // Cooldown period
                            val remainingSeconds = EmailVerificationRateLimit.COOLDOWN_SECONDS - secondsSinceLastRequest
                            logger.warn { "Rate limit cooldown for user: $userId, remaining: ${remainingSeconds}s" }
                            Mono.error(BusinessException(ErrorCode.VERIFICATION_COOLDOWN))
                        } else {
                            // Max requests reached
                            logger.warn { "Rate limit exceeded for user: $userId, count: ${rateLimit.requestCount}" }
                            Mono.error(BusinessException(ErrorCode.TOO_MANY_REQUESTS))
                        }
                    }
                    else -> {
                        // Update rate limit
                        rateLimitRepository
                            .save(rateLimit.incrementCount())
                            .thenReturn(Unit)
                            .doOnSuccess {
                                logger.debug { "Rate limit updated for user: $userId, count: ${rateLimit.requestCount + 1}" }
                            }
                    }
                }
            }.switchIfEmpty(
                // 첫 요청이면 rate limit 생성하고 통과
                rateLimitRepository
                    .save(EmailVerificationRateLimit(userId = userIdStr))
                    .thenReturn(Unit)
                    .doOnSuccess {
                        logger.debug { "Rate limit created for user: $userId" }
                    },
            )
    }

    private fun generateCode(): String = (100000..999999).random().toString()
}
