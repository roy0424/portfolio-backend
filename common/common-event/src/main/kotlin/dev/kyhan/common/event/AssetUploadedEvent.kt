package dev.kyhan.common.event

import java.time.Instant

data class AssetUploadedEvent(
    override val eventId: String,
    val assetId: String,
    val userId: String,
    val siteId: String?,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    override val timestamp: Instant = Instant.now(),
) : BaseEvent(eventId, "ASSET_UPLOADED", timestamp)
