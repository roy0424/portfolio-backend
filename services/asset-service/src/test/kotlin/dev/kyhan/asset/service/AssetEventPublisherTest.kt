package dev.kyhan.asset.service

import dev.kyhan.asset.domain.Asset
import dev.kyhan.asset.domain.AssetStatus
import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.common.event.AssetDeletedEvent
import dev.kyhan.common.event.AssetUploadedEvent
import dev.kyhan.common.event.AssetVisibilityUpdatedEvent
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import java.time.Instant
import java.util.UUID

@DisplayName("AssetEventPublisher 테스트")
class AssetEventPublisherTest {
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    private lateinit var publisher: AssetEventPublisher

    private val userId = UUID.randomUUID()
    private val siteId = UUID.randomUUID()
    private val assetId = UUID.randomUUID()
    private val now = Instant.now()

    private val asset =
        Asset(
            id = assetId,
            userId = userId,
            siteId = siteId,
            fileName = "test-image.jpg",
            originalFileName = "my-photo.jpg",
            contentType = "image/jpeg",
            fileSize = 1024000L,
            storagePath = "public/$userId/$assetId/my-photo.jpg",
            visibility = AssetVisibility.PUBLIC,
            status = AssetStatus.ACTIVE,
            uploadedAt = now,
        )

    @BeforeEach
    fun setUp() {
        kafkaTemplate = mockk(relaxed = true)
        publisher = AssetEventPublisher(kafkaTemplate)
    }

    @Nested
    @DisplayName("publishAssetUploaded")
    inner class PublishAssetUploaded {
        @Test
        fun `AssetUploadedEvent 발행 성공`() {
            val eventSlot = slot<AssetUploadedEvent>()

            publisher.publishAssetUploaded(asset)

            verify(exactly = 1) {
                kafkaTemplate.send("asset-events", assetId.toString(), capture(eventSlot))
            }

            val event = eventSlot.captured
            assert(event.assetId == assetId.toString())
            assert(event.userId == userId.toString())
            assert(event.siteId == siteId.toString())
            assert(event.fileName == "test-image.jpg")
            assert(event.contentType == "image/jpeg")
            assert(event.fileSize == 1024000L)
            assert(event.eventType == "ASSET_UPLOADED")
        }

        @Test
        fun `siteId가 null인 경우`() {
            val assetWithoutSite = asset.copy(siteId = null)
            val eventSlot = slot<AssetUploadedEvent>()

            publisher.publishAssetUploaded(assetWithoutSite)

            verify(exactly = 1) {
                kafkaTemplate.send("asset-events", assetId.toString(), capture(eventSlot))
            }

            val event = eventSlot.captured
            assert(event.siteId == null)
        }
    }

    @Nested
    @DisplayName("publishAssetDeleted")
    inner class PublishAssetDeleted {
        @Test
        fun `AssetDeletedEvent 발행 성공`() {
            val eventSlot = slot<AssetDeletedEvent>()

            publisher.publishAssetDeleted(asset)

            verify(exactly = 1) {
                kafkaTemplate.send("asset-events", assetId.toString(), capture(eventSlot))
            }

            val event = eventSlot.captured
            assert(event.assetId == assetId.toString())
            assert(event.userId == userId.toString())
            assert(event.siteId == siteId.toString())
            assert(event.eventType == "ASSET_DELETED")
        }
    }

    @Nested
    @DisplayName("publishVisibilityUpdated")
    inner class PublishVisibilityUpdated {
        @Test
        fun `AssetVisibilityUpdatedEvent 발행 성공`() {
            val eventSlot = slot<AssetVisibilityUpdatedEvent>()
            val oldVisibility = AssetVisibility.PRIVATE
            val newVisibility = AssetVisibility.PUBLIC

            publisher.publishVisibilityUpdated(asset, oldVisibility, newVisibility)

            verify(exactly = 1) {
                kafkaTemplate.send("asset-events", assetId.toString(), capture(eventSlot))
            }

            val event = eventSlot.captured
            assert(event.assetId == assetId.toString())
            assert(event.oldVisibility == "PRIVATE")
            assert(event.newVisibility == "PUBLIC")
            assert(event.eventType == "ASSET_VISIBILITY_UPDATED")
        }
    }

    @Nested
    @DisplayName("Kafka 전송")
    inner class KafkaSend {
        @Test
        fun `모든 이벤트가 asset-events 토픽으로 전송됨`() {
            publisher.publishAssetUploaded(asset)
            publisher.publishAssetDeleted(asset)
            publisher.publishVisibilityUpdated(asset, AssetVisibility.PRIVATE, AssetVisibility.PUBLIC)

            verify(exactly = 3) {
                kafkaTemplate.send("asset-events", any<String>(), any())
            }
        }

        @Test
        fun `이벤트 key는 assetId`() {
            publisher.publishAssetUploaded(asset)

            verify(exactly = 1) {
                kafkaTemplate.send("asset-events", assetId.toString(), any())
            }
        }
    }
}
