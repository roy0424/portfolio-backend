package dev.kyhan.auth.service

import dev.kyhan.asset.grpc.VerifyAssetOwnershipResponse
import dev.kyhan.auth.domain.UserProfile
import dev.kyhan.auth.dto.UpdateUserProfileRequest
import dev.kyhan.auth.grpc.AssetServiceClient
import dev.kyhan.auth.repository.UserProfileRepository
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import dev.kyhan.common.exception.NotFoundException
import io.mockk.coEvery
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
    private lateinit var assetServiceClient: AssetServiceClient
    private lateinit var kafkaTemplate: org.springframework.kafka.core.KafkaTemplate<String, Any>
    private lateinit var service: UserProfileService

    private val userId = UUID.randomUUID()
    private val profileId = UUID.randomUUID()
    private val assetId = UUID.randomUUID()
    private val now = Instant.now()

    private val activeProfile =
        UserProfile(
            id = profileId,
            userId = userId,
            displayName = "Tester",
            avatarAssetId = assetId,
            bio = "Hi",
            website = "https://example.com",
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )

    @BeforeEach
    fun setUp() {
        userProfileRepository = mockk(relaxed = true)
        assetServiceClient = mockk(relaxed = true)
        kafkaTemplate = mockk(relaxed = true)

        // Mock assetServiceClient의 verifyAssetOwnership을 항상 성공으로 설정
        coEvery { assetServiceClient.verifyAssetOwnership(assetId, any()) } returns
            VerifyAssetOwnershipResponse
                .newBuilder()
                .setExists(true)
                .setIsOwner(true)
                .setCdnUrl("https://cdn.example.com/avatar.jpg")
                .build()

        service = UserProfileService(userProfileRepository, assetServiceClient, kafkaTemplate)
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
                        dto.displayName == "Tester" &&
                        dto.avatarUrl == "https://cdn.example.com/avatar.jpg"
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
        fun `프로필 업데이트 성공 (avatarAssetId 없이)`() {
            val request =
                UpdateUserProfileRequest(
                    displayName = "NewName",
                    avatarAssetId = null,
                    bio = "New bio",
                    website = "https://site.example.com",
                )
            val updatedProfile =
                activeProfile.copy(
                    displayName = "NewName",
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
                        dto.bio == request.bio &&
                        dto.website == request.website
                }.verifyComplete()

            verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
            verify(exactly = 1) { userProfileRepository.save(any()) }
        }

        @Test
        fun `프로필 업데이트 성공 (avatarAssetId 포함)`() {
            val newAssetId = UUID.randomUUID()
            val request =
                UpdateUserProfileRequest(
                    displayName = "NewName",
                    avatarAssetId = newAssetId.toString(),
                    bio = "New bio",
                    website = "https://site.example.com",
                )

            val grpcResponse =
                VerifyAssetOwnershipResponse
                    .newBuilder()
                    .setExists(true)
                    .setIsOwner(true)
                    .setCdnUrl("https://cdn.example.com/new-avatar.png")
                    .build()

            val updatedProfile =
                activeProfile.copy(
                    displayName = "NewName",
                    avatarAssetId = newAssetId,
                    bio = request.bio,
                    website = request.website,
                    updatedAt = now.plusSeconds(10),
                )

            every { userProfileRepository.findByUserId(userId) } returns Mono.just(activeProfile)
            coEvery { assetServiceClient.verifyAssetOwnership(newAssetId, userId) } returns grpcResponse
            every { userProfileRepository.save(any()) } returns Mono.just(updatedProfile)

            StepVerifier
                .create(service.updateProfile(userId, request))
                .expectNextMatches { dto ->
                    dto.displayName == "NewName" &&
                        dto.avatarUrl == "https://cdn.example.com/new-avatar.png" &&
                        dto.bio == request.bio &&
                        dto.website == request.website
                }.verifyComplete()

            verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
            verify(exactly = 1) { userProfileRepository.save(any()) }
            verify(exactly = 1) { kafkaTemplate.send("asset-commands", any()) }
        }

        @Test
        fun `avatar 교체 시 이전 asset 삭제 명령 발행`() {
            val oldAssetId = UUID.randomUUID()
            val newAssetId = UUID.randomUUID()

            val profileWithOldAvatar =
                activeProfile.copy(
                    avatarAssetId = oldAssetId,
                )

            val request =
                UpdateUserProfileRequest(
                    avatarAssetId = newAssetId.toString(),
                )

            val grpcResponse =
                VerifyAssetOwnershipResponse
                    .newBuilder()
                    .setExists(true)
                    .setIsOwner(true)
                    .setCdnUrl("https://cdn.example.com/new-avatar.png")
                    .build()

            val updatedProfile =
                profileWithOldAvatar.copy(
                    avatarAssetId = newAssetId,
                    updatedAt = now.plusSeconds(10),
                )

            every { userProfileRepository.findByUserId(userId) } returns Mono.just(profileWithOldAvatar)
            coEvery { assetServiceClient.verifyAssetOwnership(newAssetId, userId) } returns grpcResponse
            every { userProfileRepository.save(any()) } returns Mono.just(updatedProfile)

            StepVerifier
                .create(service.updateProfile(userId, request))
                .expectNextMatches { dto ->
                    dto.avatarUrl == "https://cdn.example.com/new-avatar.png"
                }.verifyComplete()

            // Verify DeleteAssetCommand was published for old asset
            verify(exactly = 1) {
                kafkaTemplate.send(
                    "asset-commands",
                    match { cmd ->
                        cmd is dev.kyhan.common.event.DeleteAssetCommand &&
                            cmd.assetId == oldAssetId.toString() &&
                            cmd.userId == userId.toString() &&
                            cmd.reason == "PROFILE_AVATAR_REPLACED"
                    },
                )
            }
        }

        @Test
        fun `avatarAssetId가 존재하지 않으면 ASSET_NOT_FOUND`() {
            val newAssetId = UUID.randomUUID()
            val request =
                UpdateUserProfileRequest(
                    displayName = "NewName",
                    avatarAssetId = newAssetId.toString(),
                )

            val grpcResponse =
                VerifyAssetOwnershipResponse
                    .newBuilder()
                    .setExists(false)
                    .setIsOwner(false)
                    .setCdnUrl("")
                    .build()

            every { userProfileRepository.findByUserId(userId) } returns Mono.just(activeProfile)
            coEvery { assetServiceClient.verifyAssetOwnership(newAssetId, userId) } returns grpcResponse

            StepVerifier
                .create(service.updateProfile(userId, request))
                .expectErrorSatisfies { error ->
                    assert(error is InvalidInputException)
                    val ex = error as InvalidInputException
                    assert(ex.errorCode == ErrorCode.ASSET_NOT_FOUND)
                }.verify()
        }

        @Test
        fun `avatarAssetId가 다른 사용자 소유면 INVALID_INPUT`() {
            val newAssetId = UUID.randomUUID()
            val request =
                UpdateUserProfileRequest(
                    displayName = "NewName",
                    avatarAssetId = newAssetId.toString(),
                )

            val grpcResponse =
                VerifyAssetOwnershipResponse
                    .newBuilder()
                    .setExists(true)
                    .setIsOwner(false)
                    .setCdnUrl("")
                    .build()

            every { userProfileRepository.findByUserId(userId) } returns Mono.just(activeProfile)
            coEvery { assetServiceClient.verifyAssetOwnership(newAssetId, userId) } returns grpcResponse

            StepVerifier
                .create(service.updateProfile(userId, request))
                .expectErrorSatisfies { error ->
                    assert(error is InvalidInputException)
                    val ex = error as InvalidInputException
                    assert(ex.errorCode == ErrorCode.INVALID_INPUT)
                }.verify()
        }

        @Test
        fun `잘못된 avatarAssetId 형식이면 INVALID_INPUT`() {
            val request =
                UpdateUserProfileRequest(
                    displayName = "NewName",
                    avatarAssetId = "invalid-uuid",
                )

            every { userProfileRepository.findByUserId(userId) } returns Mono.just(activeProfile)

            StepVerifier
                .create(service.updateProfile(userId, request))
                .expectErrorSatisfies { error ->
                    assert(error is InvalidInputException)
                    val ex = error as InvalidInputException
                    assert(ex.errorCode == ErrorCode.INVALID_INPUT)
                    assert(ex.message?.contains("Invalid avatar asset ID format") == true)
                }.verify()
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
        fun `잘못된 website URL이면 INVALID_URL_FORMAT`() {
            val badRequest = UpdateUserProfileRequest(website = "ftp://invalid")

            val ex =
                assertThrows<InvalidInputException> {
                    service.updateProfile(userId, badRequest)
                }
            assert(ex.errorCode == ErrorCode.INVALID_URL_FORMAT)
        }
    }
}
