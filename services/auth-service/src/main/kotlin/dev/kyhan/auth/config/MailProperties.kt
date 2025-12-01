package dev.kyhan.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "mail")
data class MailProperties(
    var fromAddress: String = "noreply@portfolio-platform.com",
    var fromName: String = "Portfolio Platform",
)
