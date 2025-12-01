package dev.kyhan.common.event

import java.time.Instant

data class PageViewedEvent(
    override val eventId: String,
    val siteId: String,
    val pagePath: String,
    val ipAddress: String,
    val userAgent: String,
    val referer: String?,
    override val timestamp: Instant = Instant.now(),
) : BaseEvent(eventId, "PAGE_VIEWED", timestamp)
