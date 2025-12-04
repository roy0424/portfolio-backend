package dev.kyhan.auth.grpc

import dev.kyhan.asset.grpc.AssetServiceGrpcKt
import dev.kyhan.asset.grpc.VerifyAssetOwnershipRequest
import dev.kyhan.asset.grpc.VerifyAssetOwnershipResponse
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssetServiceClient {
    @GrpcClient("asset-service")
    private lateinit var assetServiceStub: AssetServiceGrpcKt.AssetServiceCoroutineStub

    suspend fun verifyAssetOwnership(
        assetId: UUID,
        userId: UUID,
    ): VerifyAssetOwnershipResponse {
        val request =
            VerifyAssetOwnershipRequest
                .newBuilder()
                .setAssetId(assetId.toString())
                .setUserId(userId.toString())
                .build()

        return assetServiceStub.verifyAssetOwnership(request)
    }
}
