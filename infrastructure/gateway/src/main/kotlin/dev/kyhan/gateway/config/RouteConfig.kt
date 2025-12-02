package dev.kyhan.gateway.config

import dev.kyhan.gateway.filter.JwtAuthenticationFilter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RouteConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator =
        builder
            .routes()
            // OAuth2 Login - Auth Service로 프록시
            .route("oauth2-login") { r ->
                r
                    .path("/oauth2/**", "/login/**")
                    .uri("http://localhost:8081")
            }
            // Auth Service - Public (인증 불필요)
            .route("auth-public") { r ->
                r
                    .path("/auth/refresh")
                    .uri("http://localhost:8081")
            }
            // Auth Service - Protected (JWT 필요)
            .route("auth-protected") { r ->
                r
                    .path("/auth/email/**")
                    .filters { f -> f.filter(jwtAuthenticationFilter) }
                    .uri("http://localhost:8081")
            }
            // User Profile - Public GET by userId
            .route("user-profile-public") { r ->
                r
                    .path("/user-profile/{userId}")
                    .uri("http://localhost:8081")
            }
            // User Profile - Protected (current user operations)
            .route("user-profile-protected") { r ->
                r
                    .path("/user-profile", "/user-profile/**")
                    .filters { f -> f.filter(jwtAuthenticationFilter) }
                    .uri("http://localhost:8081")
            }
            // Portfolio Service - JWT 필요
            .route("portfolio-service") { r ->
                r
                    .path("/portfolio/**")
                    .filters { f -> f.filter(jwtAuthenticationFilter) }
                    .uri("http://localhost:8082")
            }
            // Page Service - JWT 필요
            .route("page-service") { r ->
                r
                    .path("/page/**")
                    .filters { f -> f.filter(jwtAuthenticationFilter) }
                    .uri("http://localhost:8083")
            }
            // Asset Service - JWT 필요
            .route("asset-service") { r ->
                r
                    .path("/assets/**")
                    .filters { f -> f.filter(jwtAuthenticationFilter) }
                    .uri("http://localhost:8084")
            }
            // OpenAPI docs from services
            .route("auth-service-docs") { r ->
                r
                    .path("/auth-service/v3/api-docs/**")
                    .filters { f -> f.rewritePath("/auth-service/v3/api-docs(?<segment>.*)", "/v3/api-docs\${segment}") }
                    .uri("http://localhost:8081")
            }.route("portfolio-service-docs") { r ->
                r
                    .path("/portfolio-service/v3/api-docs/**")
                    .filters { f -> f.rewritePath("/portfolio-service/v3/api-docs(?<segment>.*)", "/v3/api-docs\${segment}") }
                    .uri("http://localhost:8082")
            }.route("page-service-docs") { r ->
                r
                    .path("/page-service/v3/api-docs/**")
                    .filters { f -> f.rewritePath("/page-service/v3/api-docs(?<segment>.*)", "/v3/api-docs\${segment}") }
                    .uri("http://localhost:8083")
            }.route("asset-service-docs") { r ->
                r
                    .path("/asset-service/v3/api-docs/**")
                    .filters { f -> f.rewritePath("/asset-service/v3/api-docs(?<segment>.*)", "/v3/api-docs\${segment}") }
                    .uri("http://localhost:8084")
            }.build()
}
