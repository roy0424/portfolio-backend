package dev.kyhan.auth.repository

import dev.kyhan.auth.domain.EmailVerificationRateLimit
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

interface EmailVerificationRateLimitRepository {
    fun save(rateLimit: EmailVerificationRateLimit): Mono<EmailVerificationRateLimit>

    fun findById(userId: String): Mono<EmailVerificationRateLimit>

    fun deleteById(userId: UUID): Mono<Boolean>
}

@Repository
class EmailVerificationRateLimitRepositoryImpl(
    @Qualifier("emailVerificationRateLimitRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, EmailVerificationRateLimit>,
) : EmailVerificationRateLimitRepository {
    companion object {
        private const val KEY_PREFIX = "email_verification_rate_limit:"
    }

    override fun save(rateLimit: EmailVerificationRateLimit): Mono<EmailVerificationRateLimit> {
        val key = KEY_PREFIX + rateLimit.userId
        return redisTemplate
            .opsForValue()
            .set(key, rateLimit, Duration.ofSeconds(rateLimit.ttl))
            .thenReturn(rateLimit)
    }

    override fun findById(userId: String): Mono<EmailVerificationRateLimit> {
        val key = KEY_PREFIX + userId
        return redisTemplate.opsForValue().get(key)
    }

    override fun deleteById(userId: UUID): Mono<Boolean> {
        val key = KEY_PREFIX + userId.toString()
        return redisTemplate.delete(key).map { it > 0 }
    }
}
