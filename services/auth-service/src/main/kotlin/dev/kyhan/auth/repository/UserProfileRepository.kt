package dev.kyhan.auth.repository

import dev.kyhan.auth.domain.UserProfile
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface UserProfileRepository : ReactiveCrudRepository<UserProfile, UUID> {
    /**
     * Find active (non-deleted) user profile by user ID
     */
    @Query("SELECT * FROM auth.user_profile WHERE user_id = :userId AND deleted_at IS NULL")
    fun findActiveByUserId(userId: UUID): Mono<UserProfile>

    /**
     * Find user profile by user ID (including deleted profiles)
     * Used for checking if profile exists even if deleted
     */
    fun findByUserId(userId: UUID): Mono<UserProfile>
}
