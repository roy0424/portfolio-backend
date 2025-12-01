package dev.kyhan.auth.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dev.kyhan.auth.domain.EmailVerificationCode
import dev.kyhan.auth.domain.EmailVerificationRateLimit
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig(
    private val objectMapper: ObjectMapper,
) {
    init {
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Bean
    fun emailVerificationCodeRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<String, EmailVerificationCode> {
        val serializer = Jackson2JsonRedisSerializer(objectMapper, EmailVerificationCode::class.java)
        val context =
            RedisSerializationContext
                .newSerializationContext<String, EmailVerificationCode>(StringRedisSerializer())
                .value(serializer)
                .build()

        return ReactiveRedisTemplate(connectionFactory, context)
    }

    @Bean
    fun emailVerificationRateLimitRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<String, EmailVerificationRateLimit> {
        val serializer = Jackson2JsonRedisSerializer(objectMapper, EmailVerificationRateLimit::class.java)
        val context =
            RedisSerializationContext
                .newSerializationContext<String, EmailVerificationRateLimit>(StringRedisSerializer())
                .value(serializer)
                .build()

        return ReactiveRedisTemplate(connectionFactory, context)
    }
}
