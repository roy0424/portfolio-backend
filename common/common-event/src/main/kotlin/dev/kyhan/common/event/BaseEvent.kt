package dev.kyhan.common.event

import java.time.Instant

abstract class BaseEvent(
    open val eventId: String,
    open val eventType: String,
    open val timestamp: Instant = Instant.now()
)