package dev.kyhan.auth.service

import dev.kyhan.auth.dto.UpdateUserProfileRequest
import dev.kyhan.auth.dto.UserProfileDto
import dev.kyhan.auth.grpc.AssetServiceClient
import dev.kyhan.auth.repository.UserProfileRepository
import dev.kyhan.common.event.DeleteAssetCommand
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import dev.kyhan.common.exception.NotFoundException
import dev.kyhan.common.util.ValidationUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class UserProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val assetServiceClient: AssetServiceClient,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    fun getMyProfile(userId: UUID): Mono<UserProfileDto> =
        userProfileRepository
            .findActiveByUserId(userId)
            .flatMap { profile -> buildProfileDto(profile, userId) }
            .switchIfEmpty(Mono.defer { handleMissingProfile(userId) })

    fun getProfileByUserId(userId: UUID): Mono<UserProfileDto> =
        userProfileRepository
            .findActiveByUserId(userId)
            .flatMap { profile -> buildProfileDto(profile, userId) }
            .switchIfEmpty(Mono.defer { handleMissingProfile(userId) })

    private fun buildProfileDto(
        profile: dev.kyhan.auth.domain.UserProfile,
        userId: UUID,
    ): Mono<UserProfileDto> {
        // avatar가 없으면 그냥 바로 DTO 리턴
        if (profile.avatarAssetId == null) {
            return Mono.just(UserProfileDto.from(profile, null))
        }

        // avatar가 있을 때만 gRPC 호출
        return mono {
            try {
                val response = assetServiceClient.verifyAssetOwnership(profile.avatarAssetId, userId)
                val avatarUrl =
                    if (response.exists && response.isOwner) response.cdnUrl else null

                UserProfileDto.from(profile, avatarUrl)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to fetch avatar URL for assetId=${profile.avatarAssetId}" }
                UserProfileDto.from(profile, null)
            }
        }
    }

    fun updateProfile(
        userId: UUID,
        request: UpdateUserProfileRequest,
    ): Mono<UserProfileDto> {
        validateUrls(request.website)

        return userProfileRepository
            .findByUserId(userId)
            .switchIfEmpty(Mono.error(NotFoundException(ErrorCode.USER_PROFILE_NOT_FOUND)))
            .flatMap { profile ->
                if (profile.deletedAt != null) {
                    return@flatMap Mono.error<UserProfileDto>(BusinessException(ErrorCode.USER_PROFILE_DELETED))
                }

                // If avatarAssetId is provided, verify ownership via gRPC
                val avatarUpdateMono =
                    if (request.avatarAssetId != null) {
                        mono {
                            val assetId = parseAssetId(request.avatarAssetId)
                            val response = assetServiceClient.verifyAssetOwnership(assetId, userId)

                            if (!response.exists) {
                                throw InvalidInputException(ErrorCode.ASSET_NOT_FOUND, "Asset not found")
                            }

                            if (!response.isOwner) {
                                throw InvalidInputException(ErrorCode.INVALID_INPUT, "Asset does not belong to user")
                            }

                            logger.info { "Avatar asset verified: assetId=$assetId, userId=$userId" }

                            // Return assetId and URL for response
                            Pair(assetId, response.cdnUrl)
                        }
                    } else {
                        Mono.just(Pair(profile.avatarAssetId, null))
                    }

                avatarUpdateMono.flatMap { (newAvatarAssetId, avatarUrl) ->
                    // If avatar changed, schedule old asset for deletion
                    val oldAvatarAssetId = profile.avatarAssetId
                    if (oldAvatarAssetId != null && newAvatarAssetId != null && oldAvatarAssetId != newAvatarAssetId) {
                        publishDeleteAssetCommand(oldAvatarAssetId, userId, "PROFILE_AVATAR_REPLACED")
                    }

                    val updatedProfile =
                        profile.copy(
                            displayName = request.displayName ?: profile.displayName,
                            avatarAssetId = newAvatarAssetId,
                            bio = request.bio ?: profile.bio,
                            website = request.website ?: profile.website,
                            updatedAt = Instant.now(),
                        )

                    userProfileRepository
                        .save(updatedProfile)
                        .map { UserProfileDto.from(it, avatarUrl) }
                }
            }
    }

    private fun parseAssetId(assetIdStr: String): UUID {
        try {
            return UUID.fromString(assetIdStr)
        } catch (e: IllegalArgumentException) {
            throw InvalidInputException(ErrorCode.INVALID_INPUT, "Invalid avatar asset ID format")
        }
    }

    private fun publishDeleteAssetCommand(
        assetId: UUID,
        userId: UUID,
        reason: String,
    ) {
        try {
            val command =
                DeleteAssetCommand(
                    assetId = assetId.toString(),
                    userId = userId.toString(),
                    reason = reason,
                )
            kafkaTemplate.send("asset-commands", command)
            logger.info { "Published DeleteAssetCommand: assetId=$assetId, userId=$userId, reason=$reason" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish DeleteAssetCommand: assetId=$assetId" }
            // Don't fail the profile update if Kafka publish fails
        }
    }

    private fun handleMissingProfile(userId: UUID): Mono<UserProfileDto> =
        userProfileRepository
            .findByUserId(userId)
            .flatMap { Mono.error<UserProfileDto>(BusinessException(ErrorCode.USER_PROFILE_DELETED)) }
            .switchIfEmpty(Mono.error(NotFoundException(ErrorCode.USER_PROFILE_NOT_FOUND)))

    private fun validateUrls(website: String?) {
        if (!website.isNullOrBlank() && !ValidationUtils.isValidUrl(website)) {
            throw InvalidInputException(ErrorCode.INVALID_URL_FORMAT, "Website must start with http:// or https://")
        }
    }
}
