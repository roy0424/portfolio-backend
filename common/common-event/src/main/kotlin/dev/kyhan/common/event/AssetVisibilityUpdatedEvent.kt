package dev.kyhan.common.event

import java.time.Instant

data class AssetVisibilityUpdatedEvent(
    override val eventId: String,
    val assetId: String,
    val oldVisibility: String,
    val newVisibility: String,
    override val timestamp: Instant = Instant.now(),
) : BaseEvent(eventId, "ASSET_VISIBILITY_UPDATED", timestamp)
