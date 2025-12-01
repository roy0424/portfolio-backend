package dev.kyhan.auth.domain

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("EmailVerificationRateLimit 도메인 로직 테스트")
class EmailVerificationRateLimitTest {
    private val testUserId = "test-user-id"

    @Nested
    @DisplayName("canRequest 테스트")
    inner class CanRequestTest {
        @Test
        @DisplayName("성공: 첫 번째 요청 (requestCount = 1)")
        fun canRequest_FirstRequest() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 1,
                    lastRequestTime = Instant.now().minusSeconds(120), // 2분 전
                )

            // When & Then
            assertTrue(rateLimit.canRequest())
        }

        @Test
        @DisplayName("성공: 쿨다운 이후 두 번째 요청")
        fun canRequest_AfterCooldown() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 2,
                    lastRequestTime = Instant.now().minusSeconds(120), // 2분 전
                )

            // When & Then
            assertTrue(rateLimit.canRequest())
        }

        @Test
        @DisplayName("성공: 쿨다운 직후 (정확히 60초)")
        fun canRequest_ExactlyCooldownPeriod() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 1,
                    lastRequestTime = Instant.now().minusSeconds(60), // 정확히 60초 전
                )

            // When & Then
            assertTrue(rateLimit.canRequest())
        }

        @Test
        @DisplayName("성공: 최대 요청 직전 (requestCount = 4)")
        fun canRequest_BeforeMaxRequests() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 4,
                    lastRequestTime = Instant.now().minusSeconds(120),
                )

            // When & Then
            assertTrue(rateLimit.canRequest())
        }

        @Test
        @DisplayName("실패: 쿨다운 기간 내 (30초 전)")
        fun canRequest_WithinCooldown() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 1,
                    lastRequestTime = Instant.now().minusSeconds(30), // 30초 전
                )

            // When & Then
            assertFalse(rateLimit.canRequest())
        }

        @Test
        @DisplayName("실패: 쿨다운 기간 내 (방금)")
        fun canRequest_JustNow() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 1,
                    lastRequestTime = Instant.now(), // 지금
                )

            // When & Then
            assertFalse(rateLimit.canRequest())
        }

        @Test
        @DisplayName("실패: 쿨다운 직전 (59초)")
        fun canRequest_JustBeforeCooldown() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 1,
                    lastRequestTime = Instant.now().minusSeconds(59), // 59초 전
                )

            // When & Then
            assertFalse(rateLimit.canRequest())
        }

        @Test
        @DisplayName("실패: 최대 요청 횟수 도달 (requestCount = 5)")
        fun canRequest_MaxRequestsReached() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 5, // MAX_REQUESTS_PER_HOUR
                    lastRequestTime = Instant.now().minusSeconds(120),
                )

            // When & Then
            assertFalse(rateLimit.canRequest())
        }

        @Test
        @DisplayName("실패: 최대 요청 횟수 초과 (requestCount = 6)")
        fun canRequest_MaxRequestsExceeded() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 6,
                    lastRequestTime = Instant.now().minusSeconds(120),
                )

            // When & Then
            assertFalse(rateLimit.canRequest())
        }

        @Test
        @DisplayName("실패: 최대 요청 도달 + 쿨다운 내")
        fun canRequest_MaxRequestsAndWithinCooldown() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 5,
                    lastRequestTime = Instant.now().minusSeconds(30),
                )

            // When & Then
            assertFalse(rateLimit.canRequest())
        }
    }

    @Nested
    @DisplayName("incrementCount 테스트")
    inner class IncrementCountTest {
        @Test
        @DisplayName("성공: requestCount 증가")
        fun incrementCount_IncrementsCount() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 1,
                    lastRequestTime = Instant.now().minusSeconds(120),
                )

            // When
            val incremented = rateLimit.incrementCount()

            // Then
            assertEquals(2, incremented.requestCount)
            assertEquals(testUserId, incremented.userId)
        }

        @Test
        @DisplayName("성공: lastRequestTime 업데이트")
        fun incrementCount_UpdatesTimestamp() {
            // Given
            val oldTime = Instant.now().minusSeconds(120)
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 1,
                    lastRequestTime = oldTime,
                )

            // When
            val incremented = rateLimit.incrementCount()

            // Then
            assertTrue(incremented.lastRequestTime.isAfter(oldTime))
        }

        @Test
        @DisplayName("성공: 여러 번 증가")
        fun incrementCount_MultipleIncrements() {
            // Given
            val rateLimit =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 1,
                    lastRequestTime = Instant.now().minusSeconds(120),
                )

            // When
            val first = rateLimit.incrementCount()
            val second = first.incrementCount()
            val third = second.incrementCount()

            // Then
            assertEquals(2, first.requestCount)
            assertEquals(3, second.requestCount)
            assertEquals(4, third.requestCount)
            assertEquals(testUserId, third.userId)
        }

        @Test
        @DisplayName("성공: 불변성 유지 (원본 객체 변경 없음)")
        fun incrementCount_Immutability() {
            // Given
            val original =
                EmailVerificationRateLimit(
                    userId = testUserId,
                    requestCount = 1,
                    lastRequestTime = Instant.now().minusSeconds(120),
                )
            val originalCount = original.requestCount
            val originalTime = original.lastRequestTime

            // When
            val incremented = original.incrementCount()

            // Then
            assertEquals(originalCount, original.requestCount)
            assertEquals(originalTime, original.lastRequestTime)
            assertEquals(originalCount + 1, incremented.requestCount)
        }
    }

    @Nested
    @DisplayName("상수 값 테스트")
    inner class ConstantsTest {
        @Test
        @DisplayName("MAX_REQUESTS_PER_HOUR = 5")
        fun maxRequestsPerHour() {
            assertEquals(5, EmailVerificationRateLimit.MAX_REQUESTS_PER_HOUR)
        }

        @Test
        @DisplayName("COOLDOWN_SECONDS = 60")
        fun cooldownSeconds() {
            assertEquals(60, EmailVerificationRateLimit.COOLDOWN_SECONDS)
        }

        @Test
        @DisplayName("기본 TTL = 3600")
        fun defaultTtl() {
            val rateLimit = EmailVerificationRateLimit(userId = testUserId)
            assertEquals(3600, rateLimit.ttl)
        }
    }
}