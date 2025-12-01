package dev.kyhan.auth.repository

import dev.kyhan.auth.domain.EmailVerificationCode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

interface EmailVerificationCodeRepository {
    fun save(code: EmailVerificationCode): Mono<EmailVerificationCode>

    fun findById(code: String): Mono<EmailVerificationCode>

    fun findByUserId(userId: UUID): Mono<EmailVerificationCode>

    fun deleteById(code: String): Mono<Boolean>

    fun deleteByUserId(userId: UUID): Mono<Void>
}

@Repository
class EmailVerificationCodeRepositoryImpl(
    @Qualifier("emailVerificationCodeRedisTemplate")
    private val codeRedisTemplate: ReactiveRedisTemplate<String, EmailVerificationCode>,
    private val stringRedisTemplate: ReactiveRedisTemplate<String, String>,
) : EmailVerificationCodeRepository {
    companion object {
        private const val KEY_PREFIX = "email_verification:"
        private const val USER_INDEX_PREFIX = "email_verification_user:"
    }

    override fun save(code: EmailVerificationCode): Mono<EmailVerificationCode> {
        val codeKey = KEY_PREFIX + code.code
        val userIndexKey = USER_INDEX_PREFIX + code.userId.toString()

        return codeRedisTemplate
            .opsForValue()
            .set(codeKey, code, Duration.ofSeconds(code.ttl))
            .then(stringRedisTemplate.opsForValue().set(userIndexKey, code.code, Duration.ofSeconds(code.ttl)))
            .thenReturn(code)
    }

    override fun findById(code: String): Mono<EmailVerificationCode> {
        val key = KEY_PREFIX + code
        return codeRedisTemplate.opsForValue().get(key)
    }

    override fun findByUserId(userId: UUID): Mono<EmailVerificationCode> {
        val userIndexKey = USER_INDEX_PREFIX + userId.toString()
        return stringRedisTemplate
            .opsForValue()
            .get(userIndexKey)
            .flatMap { codeString ->
                findById(codeString)
            }
    }

    override fun deleteById(code: String): Mono<Boolean> {
        val key = KEY_PREFIX + code
        return codeRedisTemplate.delete(key).map { it > 0 }
    }

    override fun deleteByUserId(userId: UUID): Mono<Void> {
        val userIndexKey = USER_INDEX_PREFIX + userId.toString()
        return stringRedisTemplate
            .opsForValue()
            .get(userIndexKey)
            .flatMap { codeString ->
                val codeKey = KEY_PREFIX + codeString
                codeRedisTemplate
                    .delete(codeKey)
                    .then(stringRedisTemplate.delete(userIndexKey))
                    .then()
            }.switchIfEmpty(Mono.empty())
    }
}
