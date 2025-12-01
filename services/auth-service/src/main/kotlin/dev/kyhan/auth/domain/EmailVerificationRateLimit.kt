package dev.kyhan.auth.domain

import java.time.Instant

data class EmailVerificationRateLimit(
    val userId: String,
    val requestCount: Int = 1,
    val lastRequestTime: Instant = Instant.now(),
    val ttl: Long = 3600 // 1 hour in seconds
) {
    companion object {
        const val MAX_REQUESTS_PER_HOUR = 5
        const val COOLDOWN_SECONDS = 60 // 1 minute
    }

    fun canRequest(): Boolean {
        val now = Instant.now()
        val secondsSinceLastRequest = now.epochSecond - lastRequestTime.epochSecond

        // Check cooldown period (1 minute)
        if (secondsSinceLastRequest < COOLDOWN_SECONDS) {
            return false
        }

        // Check max requests per hour
        return requestCount < MAX_REQUESTS_PER_HOUR
    }

    fun incrementCount(): EmailVerificationRateLimit {
        return copy(
            requestCount = requestCount + 1,
            lastRequestTime = Instant.now()
        )
    }
}