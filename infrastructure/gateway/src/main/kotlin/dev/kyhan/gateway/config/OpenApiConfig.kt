package dev.kyhan.gateway.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.cloud.gateway.route.RouteDefinitionLocator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    private val routeDefinitionLocator: RouteDefinitionLocator,
) {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Portfolio Platform - API Gateway")
                    .description("Unified API documentation for all microservices")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Portfolio Platform Team")
                            .email("support@portfolio-platform.com"),
                    ).license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT"),
                    ),
            ).servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("API Gateway - Local"),
                ),
            )

    @Bean
    fun authServiceApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("1. Auth Service")
            .pathsToMatch("/auth/**", "/oauth2/**", "/login/**")
            .build()

    @Bean
    fun portfolioServiceApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("2. Portfolio Service")
            .pathsToMatch("/portfolio/**")
            .build()

    @Bean
    fun pageServiceApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("3. Page Service")
            .pathsToMatch("/page/**")
            .build()

    @Bean
    fun assetServiceApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("4. Asset Service")
            .pathsToMatch("/assets/**")
            .build()
}
