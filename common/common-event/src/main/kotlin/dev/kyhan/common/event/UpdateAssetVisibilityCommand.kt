package dev.kyhan.common.event

import java.time.Instant

data class UpdateAssetVisibilityCommand(
    override val eventId: String,
    val assetId: String,
    val visibility: String, // "PUBLIC" or "PRIVATE"
    val requestedBy: String, // userId who requested the change
    override val timestamp: Instant = Instant.now(),
) : BaseEvent(eventId, "UPDATE_ASSET_VISIBILITY_COMMAND", timestamp)
