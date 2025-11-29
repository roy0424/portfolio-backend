package dev.kyhan.auth.config

import dev.kyhan.auth.oauth2.OAuth2LoginSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val oauth2LoginSuccessHandler: OAuth2LoginSuccessHandler
) {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/auth/**", "/login/**", "/oauth2/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.authenticationSuccessHandler(oauth2LoginSuccessHandler)
            }
            .build()
    }
}