package dev.kyhan.auth.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table(name = "user_account", schema = "auth")
data class UserAccount(
    @Id
    val id: UUID? = null,
    val email: String?,
    val provider: AuthProvider,
    val providerId: String,
    val status: UserStatus = UserStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
)

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
}

enum class AuthProvider {
    GOOGLE,
    GITHUB,
}
