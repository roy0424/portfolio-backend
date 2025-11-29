package dev.kyhan.common.event

import java.time.Instant

data class SitePublishedEvent(
    override val eventId: String,
    val siteId: String,
    val userId: String,
    val domain: String,
    override val timestamp: Instant = Instant.now()
) : BaseEvent(eventId, "SITE_PUBLISHED", timestamp)