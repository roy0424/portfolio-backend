package dev.kyhan.auth.domain

import java.util.UUID

data class EmailVerificationCode(
    val code: String,
    val userId: UUID,
    val email: String,
    val ttl: Long = 300,
)
