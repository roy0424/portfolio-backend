package dev.kyhan.common.event

import java.time.Instant

data class BulkUpdateVisibilityCommand(
    override val eventId: String,
    val assetIds: List<String>,
    val visibility: String, // "PUBLIC" or "PRIVATE"
    val requestedBy: String, // userId who requested the change
    override val timestamp: Instant = Instant.now(),
) : BaseEvent(eventId, "BULK_UPDATE_VISIBILITY_COMMAND", timestamp)
