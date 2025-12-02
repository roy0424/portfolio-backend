package dev.kyhan.auth.service

import dev.kyhan.auth.dto.UpdateUserProfileRequest
import dev.kyhan.auth.dto.UserProfileDto
import dev.kyhan.auth.repository.UserProfileRepository
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import dev.kyhan.common.exception.InvalidInputException
import dev.kyhan.common.exception.NotFoundException
import dev.kyhan.common.util.ValidationUtils
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
class UserProfileService(
    private val userProfileRepository: UserProfileRepository,
) {
    fun getMyProfile(userId: UUID): Mono<UserProfileDto> =
        userProfileRepository
            .findActiveByUserId(userId)
            .map(UserProfileDto::from)
            .switchIfEmpty(handleMissingProfile(userId))

    fun getProfileByUserId(userId: UUID): Mono<UserProfileDto> =
        userProfileRepository
            .findActiveByUserId(userId)
            .map(UserProfileDto::from)
            .switchIfEmpty(handleMissingProfile(userId))

    fun updateProfile(
        userId: UUID,
        request: UpdateUserProfileRequest,
    ): Mono<UserProfileDto> {
        validateUrls(request.avatarUrl, request.website)

        return userProfileRepository
            .findByUserId(userId)
            .switchIfEmpty(Mono.error(NotFoundException(ErrorCode.USER_PROFILE_NOT_FOUND)))
            .flatMap { profile ->
                if (profile.deletedAt != null) {
                    return@flatMap Mono.error<UserProfileDto>(BusinessException(ErrorCode.USER_PROFILE_DELETED))
                }

                val updatedProfile =
                    profile.copy(
                        displayName = request.displayName ?: profile.displayName,
                        avatarUrl = request.avatarUrl ?: profile.avatarUrl,
                        bio = request.bio ?: profile.bio,
                        website = request.website ?: profile.website,
                        updatedAt = Instant.now(),
                    )

                userProfileRepository
                    .save(updatedProfile)
                    .map(UserProfileDto::from)
            }
    }

    private fun handleMissingProfile(userId: UUID): Mono<UserProfileDto> =
        userProfileRepository
            .findByUserId(userId)
            .flatMap { Mono.error<UserProfileDto>(BusinessException(ErrorCode.USER_PROFILE_DELETED)) }
            .switchIfEmpty(Mono.error(NotFoundException(ErrorCode.USER_PROFILE_NOT_FOUND)))

    private fun validateUrls(
        avatarUrl: String?,
        website: String?,
    ) {
        if (!avatarUrl.isNullOrBlank() && !ValidationUtils.isValidUrl(avatarUrl)) {
            throw InvalidInputException(ErrorCode.INVALID_URL_FORMAT, "Avatar URL must start with http:// or https://")
        }

        if (!website.isNullOrBlank() && !ValidationUtils.isValidUrl(website)) {
            throw InvalidInputException(ErrorCode.INVALID_URL_FORMAT, "Website must start with http:// or https://")
        }
    }
}
