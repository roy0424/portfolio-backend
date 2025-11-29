package dev.kyhan.common.event

import java.time.Instant

data class BlogPostPublishedEvent(
    override val eventId: String,
    val postId: String,
    val siteId: String,
    val userId: String,
    val title: String,
    val slug: String,
    override val timestamp: Instant = Instant.now()
) : BaseEvent(eventId, "BLOG_POST_PUBLISHED", timestamp)