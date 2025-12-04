package dev.kyhan.common.event

import java.time.Instant
import java.util.UUID

/**
 * Command to delete an asset
 * Published when an asset needs to be deleted (e.g., profile avatar replaced)
 */
data class DeleteAssetCommand(
    override val eventId: String = UUID.randomUUID().toString(),
    val assetId: String,
    val userId: String,
    val reason: String, // e.g., "PROFILE_AVATAR_REPLACED"
    override val timestamp: Instant = Instant.now(),
) : BaseEvent(eventId, "DELETE_ASSET_COMMAND", timestamp)
