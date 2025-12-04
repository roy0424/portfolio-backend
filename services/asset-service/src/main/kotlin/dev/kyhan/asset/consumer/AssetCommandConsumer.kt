package dev.kyhan.asset.consumer

import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.asset.service.AssetService
import dev.kyhan.common.event.BulkUpdateVisibilityCommand
import dev.kyhan.common.event.DeleteAssetCommand
import dev.kyhan.common.event.UpdateAssetVisibilityCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class AssetCommandConsumer(
    private val assetService: AssetService,
) {
    @KafkaListener(
        topics = ["asset-commands"],
        groupId = "asset-service",
        containerFactory = "kafkaListenerContainerFactory",
    )
    suspend fun handleCommand(record: ConsumerRecord<String, Any>) {
        val message = record.value()
        logger.info { "Received asset command: ${message::class.simpleName}" }

        when (message) {
            is UpdateAssetVisibilityCommand -> handleUpdateVisibility(message)
            is BulkUpdateVisibilityCommand -> handleBulkUpdateVisibility(message)
            is DeleteAssetCommand -> handleDeleteAsset(message)
            else -> logger.warn { "Unknown command type: ${message::class.simpleName}" }
        }
    }

    private suspend fun handleUpdateVisibility(command: UpdateAssetVisibilityCommand) {
        try {
            val assetId = UUID.fromString(command.assetId)
            val requestedBy = UUID.fromString(command.requestedBy)
            val visibility = AssetVisibility.valueOf(command.visibility)

            logger.info { "Processing UpdateAssetVisibilityCommand for asset ${command.assetId}" }

            assetService.updateVisibility(assetId, requestedBy, visibility)

            logger.info { "Successfully processed UpdateAssetVisibilityCommand for asset ${command.assetId}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process UpdateAssetVisibilityCommand: ${e.message}" }
            // Event will be published by AssetService if it fails
        }
    }

    private suspend fun handleBulkUpdateVisibility(command: BulkUpdateVisibilityCommand) {
        try {
            val assetIds = command.assetIds.map { UUID.fromString(it) }
            val requestedBy = UUID.fromString(command.requestedBy)
            val visibility = AssetVisibility.valueOf(command.visibility)

            logger.info { "Processing BulkUpdateVisibilityCommand for ${assetIds.size} assets" }

            val result = assetService.bulkUpdateVisibility(assetIds, visibility, requestedBy)

            logger.info {
                "Bulk update completed: ${result.successCount} succeeded, ${result.failedIds.size} failed"
            }

            if (result.failedIds.isNotEmpty()) {
                logger.warn { "Failed asset IDs: ${result.failedIds}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process BulkUpdateVisibilityCommand: ${e.message}" }
        }
    }

    private suspend fun handleDeleteAsset(command: DeleteAssetCommand) {
        try {
            val assetId = UUID.fromString(command.assetId)
            val userId = UUID.fromString(command.userId)

            logger.info { "Processing DeleteAssetCommand for asset ${command.assetId}, reason: ${command.reason}" }

            assetService.deleteAsset(assetId, userId)

            logger.info { "Successfully deleted asset ${command.assetId}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process DeleteAssetCommand for asset ${command.assetId}: ${e.message}" }
            // Don't rethrow - we don't want to retry indefinitely for deleted assets
        }
    }
}
