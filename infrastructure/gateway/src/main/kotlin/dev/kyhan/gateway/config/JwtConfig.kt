package dev.kyhan.gateway.config

import dev.kyhan.common.security.JwtProperties
import dev.kyhan.common.security.JwtProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JwtConfig {
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
}
