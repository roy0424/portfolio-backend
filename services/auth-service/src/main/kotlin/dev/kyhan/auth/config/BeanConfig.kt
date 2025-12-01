package dev.kyhan.auth.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.kyhan.common.security.JwtProperties
import dev.kyhan.common.security.JwtProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanConfig {
    @Bean
    @ConfigurationProperties(prefix = "jwt")
    fun jwtProperties(): JwtProperties =
        JwtProperties(
            secret = "your-secret-key-change-this-in-production-at-least-32-characters",
            accessTokenExpiration = 3600000,
            refreshTokenExpiration = 2592000000,
        )

    @Bean
    fun jwtProvider(properties: JwtProperties): JwtProvider = JwtProvider(properties)

    @Bean
    fun objectMapper(): ObjectMapper =
        ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
}
