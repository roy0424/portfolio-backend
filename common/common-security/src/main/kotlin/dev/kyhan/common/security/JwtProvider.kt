package dev.kyhan.common.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import java.util.*
import javax.crypto.SecretKey

class JwtProvider(
    private val properties: JwtProperties
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(properties.secret.toByteArray())

    fun generateAccessToken(userId: UUID, email: String): String {
        return generateToken(userId.toString(), email, properties.accessTokenExpiration, TokenType.ACCESS)
    }

    fun generateRefreshToken(userId: UUID, email: String): String {
        return generateToken(userId.toString(), email, properties.refreshTokenExpiration, TokenType.REFRESH)
    }

    private fun generateToken(
        userId: String,
        email: String,
        expiration: Long,
        tokenType: TokenType
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("type", tokenType.name)
            .issuer(properties.issuer)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            parseToken(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        return UUID.fromString(parseToken(token).payload.subject)
    }

    fun getEmailFromToken(token: String): String {
        return parseToken(token).payload["email"] as String
    }

    fun getTokenType(token: String): TokenType {
        val typeStr = parseToken(token).payload["type"] as String
        return TokenType.valueOf(typeStr)
    }

    private fun parseToken(token: String): Jws<Claims> {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
        } catch (e: SignatureException) {
            throw InvalidTokenException("Invalid JWT signature")
        } catch (e: MalformedJwtException) {
            throw InvalidTokenException("Invalid JWT token")
        } catch (e: ExpiredJwtException) {
            throw TokenExpiredException("JWT token expired")
        } catch (e: UnsupportedJwtException) {
            throw InvalidTokenException("Unsupported JWT token")
        } catch (e: IllegalArgumentException) {
            throw InvalidTokenException("JWT claims string is empty")
        }
    }

    enum class TokenType {
        ACCESS, REFRESH
    }
}

class InvalidTokenException(message: String) : RuntimeException(message)
class TokenExpiredException(message: String) : RuntimeException(message)