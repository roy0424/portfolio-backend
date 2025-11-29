package dev.kyhan.auth.repository

import dev.kyhan.auth.domain.UserProfile
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface UserProfileRepository : ReactiveCrudRepository<UserProfile, String> {
    fun findByUserId(userId: String): Mono<UserProfile>
}