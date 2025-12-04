package dev.kyhan.asset.consumer

import dev.kyhan.asset.domain.AssetVisibility
import dev.kyhan.asset.dto.AssetDto
import dev.kyhan.asset.service.AssetService
import dev.kyhan.asset.service.BulkUpdateResult
import dev.kyhan.common.event.BulkUpdateVisibilityCommand
import dev.kyhan.common.event.UpdateAssetVisibilityCommand
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@DisplayName("AssetCommandConsumer 테스트")
class AssetCommandConsumerTest {
    private lateinit var assetService: AssetService
    private lateinit var consumer: AssetCommandConsumer

    private val userId = UUID.randomUUID()
    private val assetId = UUID.randomUUID()
    private val now = Instant.now()

    @BeforeEach
    fun setUp() {
        assetService = mockk(relaxed = true)
        consumer = AssetCommandConsumer(assetService)
    }

    @Nested
    @DisplayName("handleCommand - UpdateAssetVisibilityCommand")
    inner class HandleUpdateVisibilityCommand {
        @Test
        fun `UpdateAssetVisibilityCommand 처리 성공`() =
            runTest {
                val command =
                    UpdateAssetVisibilityCommand(
                        eventId = UUID.randomUUID().toString(),
                        assetId = assetId.toString(),
                        visibility = "PRIVATE",
                        requestedBy = userId.toString(),
                        timestamp = now,
                    )

                val dto =
                    mockk<AssetDto>(relaxed = true)

                coEvery { assetService.updateVisibility(assetId, userId, AssetVisibility.PRIVATE) } returns dto

                val record = ConsumerRecord("asset-commands", 0, 0L, "key", command as Any)
                consumer.handleCommand(record)

                coVerify(exactly = 1) {
                    assetService.updateVisibility(assetId, userId, AssetVisibility.PRIVATE)
                }
            }

        @Test
        fun `UpdateAssetVisibilityCommand 처리 실패해도 예외 발생 안 함`() =
            runTest {
                val command =
                    UpdateAssetVisibilityCommand(
                        eventId = UUID.randomUUID().toString(),
                        assetId = assetId.toString(),
                        visibility = "PUBLIC",
                        requestedBy = userId.toString(),
                        timestamp = now,
                    )

                coEvery { assetService.updateVisibility(any(), any(), any()) } throws RuntimeException("Update failed")

                // Should not throw exception
                val record = ConsumerRecord("asset-commands", 0, 0L, "key", command as Any)
                consumer.handleCommand(record)

                coVerify(exactly = 1) {
                    assetService.updateVisibility(any(), any(), any())
                }
            }

        @Test
        fun `잘못된 UUID 형식이면 예외 발생하지만 처리 계속`() =
            runTest {
                val command =
                    UpdateAssetVisibilityCommand(
                        eventId = UUID.randomUUID().toString(),
                        assetId = "invalid-uuid",
                        visibility = "PUBLIC",
                        requestedBy = userId.toString(),
                        timestamp = now,
                    )

                // Should not throw exception even if UUID parsing fails
                val record = ConsumerRecord("asset-commands", 0, 0L, "key", command as Any)
                consumer.handleCommand(record)
            }
    }

    @Nested
    @DisplayName("handleCommand - BulkUpdateVisibilityCommand")
    inner class HandleBulkUpdateVisibilityCommand {
        @Test
        fun `BulkUpdateVisibilityCommand 처리 성공`() =
            runTest {
                val assetId1 = UUID.randomUUID()
                val assetId2 = UUID.randomUUID()

                val command =
                    BulkUpdateVisibilityCommand(
                        eventId = UUID.randomUUID().toString(),
                        assetIds = listOf(assetId1.toString(), assetId2.toString()),
                        visibility = "PUBLIC",
                        requestedBy = userId.toString(),
                        timestamp = now,
                    )

                val result =
                    BulkUpdateResult(
                        successCount = 2,
                        failedIds = emptyList(),
                    )

                coEvery {
                    assetService.bulkUpdateVisibility(
                        listOf(assetId1, assetId2),
                        AssetVisibility.PUBLIC,
                        userId,
                    )
                } returns result

                val record = ConsumerRecord("asset-commands", 0, 0L, "key", command as Any)
                consumer.handleCommand(record)

                coVerify(exactly = 1) {
                    assetService.bulkUpdateVisibility(
                        listOf(assetId1, assetId2),
                        AssetVisibility.PUBLIC,
                        userId,
                    )
                }
            }

        @Test
        fun `BulkUpdateVisibilityCommand 일부 실패`() =
            runTest {
                val assetId1 = UUID.randomUUID()
                val assetId2 = UUID.randomUUID()

                val command =
                    BulkUpdateVisibilityCommand(
                        eventId = UUID.randomUUID().toString(),
                        assetIds = listOf(assetId1.toString(), assetId2.toString()),
                        visibility = "PRIVATE",
                        requestedBy = userId.toString(),
                        timestamp = now,
                    )

                val result =
                    BulkUpdateResult(
                        successCount = 1,
                        failedIds = listOf(assetId2.toString()),
                    )

                coEvery {
                    assetService.bulkUpdateVisibility(any(), any(), any())
                } returns result

                val record = ConsumerRecord("asset-commands", 0, 0L, "key", command as Any)
                consumer.handleCommand(record)

                coVerify(exactly = 1) {
                    assetService.bulkUpdateVisibility(any(), any(), any())
                }
            }
    }

    @Nested
    @DisplayName("handleCommand - Unknown Command")
    inner class HandleUnknownCommand {
        @Test
        fun `알 수 없는 커맨드 타입은 무시됨`() =
            runTest {
                val unknownCommand = "Unknown command string"

                // Should not throw exception
                val record = ConsumerRecord("asset-commands", 0, 0L, "key", unknownCommand as Any)
                consumer.handleCommand(record)

                // Should not call any service method
                coVerify(exactly = 0) {
                    assetService.updateVisibility(any(), any(), any())
                    assetService.bulkUpdateVisibility(any(), any(), any())
                }
            }
    }
}
