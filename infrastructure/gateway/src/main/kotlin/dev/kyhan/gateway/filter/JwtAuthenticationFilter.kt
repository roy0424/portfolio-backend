package dev.kyhan.gateway.filter

import dev.kyhan.common.security.JwtProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) : GatewayFilter {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn { "Missing or invalid Authorization header" }
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }

        val token = authHeader.substring(7)

        return try {
            if (!jwtProvider.validateToken(token)) {
                logger.warn { "Invalid JWT token" }
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                return exchange.response.setComplete()
            }

            val userId = jwtProvider.getUserIdFromToken(token)
            val email = jwtProvider.getEmailFromToken(token)

            val mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Email", email)
                .build()

            val mutatedExchange = exchange.mutate().request(mutatedRequest).build()

            chain.filter(mutatedExchange)
        } catch (e: Exception) {
            logger.error(e) { "JWT validation error: ${e.message}" }
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            exchange.response.setComplete()
        }
    }
}