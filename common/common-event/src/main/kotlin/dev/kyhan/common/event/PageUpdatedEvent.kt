package dev.kyhan.common.event

import java.time.Instant

data class PageUpdatedEvent(
    override val eventId: String,
    val pageId: String,
    val siteId: String,
    val userId: String,
    val pagePath: String,
    override val timestamp: Instant = Instant.now()
) : BaseEvent(eventId, "PAGE_UPDATED", timestamp)