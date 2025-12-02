package dev.kyhan.auth.service

import dev.kyhan.auth.domain.UserProfile
import dev.kyhan.auth.dto.UpdateUserProfileRequest
import dev.kyhan.auth.repository.UserProfileRepository
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import dev.kyhan.common.exception.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

@DisplayName("UserProfileService 테스트")
class UserProfileServiceTest {
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var service: UserProfileService

    private val userId = UUID.randomUUID()
    private val profileId = UUID.randomUUID()
    private val now = Instant.now()

    private val activeProfile =
        UserProfile(
            id = profileId,
            userId = userId,
            displayName = "Tester",
            avatarUrl = "https://example.com/avatar.png",
            bio = "Hi",
            website = "https://example.com",
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )

    @BeforeEach
    fun setUp() {
        userProfileRepository = mockk(relaxed = true)
        service = UserProfileService(userProfileRepository)
    }

    @Nested
    @DisplayName("getMyProfile / getProfileByUserId")
    inner class GetProfile {
        @Test
        fun `활성 프로필 조회 성공`() {
            every { userProfileRepository.findActiveByUserId(userId) } returns Mono.just(activeProfile)

            StepVerifier
                .create(service.getMyProfile(userId))
                .expectNextMatches { dto ->
                    dto.id == profileId.toString() &&
                        dto.userId == userId.toString() &&
                        dto.displayName == "Tester"
                }.verifyComplete()

            verify(exactly = 1) { userProfileRepository.findActiveByUserId(userId) }
        }

        @Test
        fun `삭제된 프로필 조회 시 USER_PROFILE_DELETED`() {
            val deletedProfile = activeProfile.copy(deletedAt = now)
            every { userProfileRepository.findActiveByUserId(userId) } returns Mono.empty()
            every { userProfileRepository.findByUserId(userId) } returns Mono.just(deletedProfile)

            StepVerifier
                .create(service.getProfileByUserId(userId))
                .expectErrorSatisfies { error ->
                    assert(error is BusinessException)
                    val ex = error as BusinessException
                    assert(ex.errorCode == ErrorCode.USER_PROFILE_DELETED)
                }.verify()
        }

        @Test
        fun `프로필이 없으면 NOT_FOUND`() {
            every { userProfileRepository.findActiveByUserId(userId) } returns Mono.empty()
            every { userProfileRepository.findByUserId(userId) } returns Mono.empty()

            StepVerifier
                .create(service.getMyProfile(userId))
                .expectErrorSatisfies { error ->
                    assert(error is NotFoundException)
                    val ex = error as NotFoundException
                    assert(ex.errorCode == ErrorCode.USER_PROFILE_NOT_FOUND)
                }.verify()
        }
    }

    @Nested
    @DisplayName("updateProfile")
    inner class UpdateProfile {
        @Test
        fun `프로필 업데이트 성공`() {
            val request =
                UpdateUserProfileRequest(
                    displayName = "NewName",
                    avatarUrl = "https://cdn.example.com/avatar.png",
                    bio = "New bio",
                    website = "https://site.example.com",
                )
            val updatedProfile =
                activeProfile.copy(
                    displayName = "NewName",
                    avatarUrl = request.avatarUrl,
                    bio = request.bio,
                    website = request.website,
                    updatedAt = now.plusSeconds(10),
                )

            every { userProfileRepository.findByUserId(userId) } returns Mono.just(activeProfile)
            every { userProfileRepository.save(any()) } returns Mono.just(updatedProfile)

            StepVerifier
                .create(service.updateProfile(userId, request))
                .expectNextMatches { dto ->
                    dto.displayName == "NewName" &&
                        dto.avatarUrl == request.avatarUrl &&
                        dto.bio == request.bio &&
                        dto.website == request.website
                }.verifyComplete()

            verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
            verify(exactly = 1) { userProfileRepository.save(any()) }
        }

        @Test
        fun `삭제된 프로필 업데이트 시 USER_PROFILE_DELETED`() {
            val deletedProfile = activeProfile.copy(deletedAt = now)
            every { userProfileRepository.findByUserId(userId) } returns Mono.just(deletedProfile)

            StepVerifier
                .create(service.updateProfile(userId, UpdateUserProfileRequest(displayName = "New")))
                .expectErrorSatisfies { error ->
                    assert(error is BusinessException)
                    val ex = error as BusinessException
                    assert(ex.errorCode == ErrorCode.USER_PROFILE_DELETED)
                }.verify()
        }

        @Test
        fun `프로필이 없으면 NOT_FOUND`() {
            every { userProfileRepository.findByUserId(userId) } returns Mono.empty()

            StepVerifier
                .create(service.updateProfile(userId, UpdateUserProfileRequest(displayName = "New")))
                .expectErrorSatisfies { error ->
                    assert(error is NotFoundException)
                    val ex = error as NotFoundException
                    assert(ex.errorCode == ErrorCode.USER_PROFILE_NOT_FOUND)
                }.verify()
        }

        @Test
        fun `잘못된 URL이면 INVALID_URL_FORMAT`() {
            val badRequest = UpdateUserProfileRequest(avatarUrl = "ftp://invalid")

            val ex =
                assertThrows<InvalidInputException> {
                    service.updateProfile(userId, badRequest)
                }
            assert(ex.errorCode == ErrorCode.INVALID_URL_FORMAT)
        }
    }
}
