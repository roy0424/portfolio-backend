package dev.kyhan.asset.service

import dev.kyhan.asset.domain.Asset
import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.common.event.AssetDeletedEvent
import dev.kyhan.common.event.AssetUploadedEvent
import dev.kyhan.common.event.AssetVisibilityUpdatedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class AssetEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    fun publishAssetUploaded(asset: Asset) {
        val event =
            AssetUploadedEvent(
                eventId = UUID.randomUUID().toString(),
                assetId = asset.id.toString(),
                userId = asset.userId.toString(),
                siteId = asset.siteId?.toString(),
                fileName = asset.fileName,
                contentType = asset.contentType,
                fileSize = asset.fileSize,
                timestamp = Instant.now(),
            )

        kafkaTemplate.send("asset-events", event.assetId, event)
        logger.info { "Published AssetUploadedEvent: ${event.eventId}" }
    }

    fun publishAssetDeleted(asset: Asset) {
        val event =
            AssetDeletedEvent(
                eventId = UUID.randomUUID().toString(),
                assetId = asset.id.toString(),
                userId = asset.userId.toString(),
                siteId = asset.siteId?.toString(),
                timestamp = Instant.now(),
            )

        kafkaTemplate.send("asset-events", event.assetId, event)
        logger.info { "Published AssetDeletedEvent: ${event.eventId}" }
    }

    fun publishVisibilityUpdated(
        asset: Asset,
        oldVisibility: AssetVisibility,
        newVisibility: AssetVisibility,
    ) {
        val event =
            AssetVisibilityUpdatedEvent(
                eventId = UUID.randomUUID().toString(),
                assetId = asset.id.toString(),
                oldVisibility = oldVisibility.name,
                newVisibility = newVisibility.name,
                timestamp = Instant.now(),
            )

        kafkaTemplate.send("asset-events", event.assetId, event)
        logger.info { "Published AssetVisibilityUpdatedEvent: ${event.eventId}" }
    }
}
