package dev.kyhan.auth.repository

import dev.kyhan.auth.domain.AuthProvider
import dev.kyhan.auth.domain.UserAccount
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface UserAccountRepository : ReactiveCrudRepository<UserAccount, String> {
    fun findByEmail(email: String): Mono<UserAccount>
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): Mono<UserAccount>
    fun existsByProviderAndProviderId(provider: AuthProvider, providerId: String): Mono<Boolean>
}