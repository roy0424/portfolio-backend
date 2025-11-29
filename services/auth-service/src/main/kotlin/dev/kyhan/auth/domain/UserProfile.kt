package dev.kyhan.auth.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table(name = "user_profile", schema = "auth")
data class UserProfile(
    @Id
    var id: String? = null,
    val userId: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val website: String? = null,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)