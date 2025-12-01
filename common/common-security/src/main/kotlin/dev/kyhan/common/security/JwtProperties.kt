package dev.kyhan.common.security

data class JwtProperties(
    var secret: String = "",
    var accessTokenExpiration: Long = 3600000, // 1 hour
    var refreshTokenExpiration: Long = 2592000000, // 30 days
    var issuer: String = "portfolio-platform",
)
