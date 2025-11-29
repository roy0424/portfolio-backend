package dev.kyhan.gateway.config

import dev.kyhan.gateway.filter.JwtAuthenticationFilter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RouteConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {
    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            // OAuth2 Login - Auth Service로 프록시
            .route("oauth2-login") { r ->
                r.path("/oauth2/**", "/login/**")
                    .uri("http://localhost:8081")
            }
            // Auth Service - 인증 불필요
            .route("auth-service") { r ->
                r.path("/auth/**")
                    .uri("http://localhost:8081")
            }
            // Portfolio Service - JWT 필요
            .route("portfolio-service") { r ->
                r.path("/portfolio/**")
                    .filters { f -> f.filter(jwtAuthenticationFilter) }
                    .uri("http://localhost:8082")
            }
            // Page Service - JWT 필요
            .route("page-service") { r ->
                r.path("/page/**")
                    .filters { f -> f.filter(jwtAuthenticationFilter) }
                    .uri("http://localhost:8083")
            }
            // Asset Service - JWT 필요
            .route("asset-service") { r ->
                r.path("/assets/**")
                    .filters { f -> f.filter(jwtAuthenticationFilter) }
                    .uri("http://localhost:8084")
            }
            .build()
    }
}