package dev.kyhan.common.event

import java.time.Instant

data class ProjectCreatedEvent(
    override val eventId: String,
    val projectId: String,
    val siteId: String,
    val userId: String,
    val title: String,
    override val timestamp: Instant = Instant.now()
) : BaseEvent(eventId, "PROJECT_CREATED", timestamp)