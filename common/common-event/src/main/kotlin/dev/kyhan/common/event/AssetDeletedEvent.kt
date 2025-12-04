package dev.kyhan.common.event

import java.time.Instant

data class AssetDeletedEvent(
    override val eventId: String,
    val assetId: String,
    val userId: String,
    val siteId: String?,
    override val timestamp: Instant = Instant.now(),
) : BaseEvent(eventId, "ASSET_DELETED", timestamp)
