package dev.kyhan.auth.repository

import dev.kyhan.auth.domain.UserProfile
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface UserProfileRepository : ReactiveCrudRepository<UserProfile, UUID> {
    fun findByUserId(userId: UUID): Mono<UserProfile>
}