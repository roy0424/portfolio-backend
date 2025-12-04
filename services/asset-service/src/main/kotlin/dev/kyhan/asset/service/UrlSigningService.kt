package dev.kyhan.asset.service

import dev.kyhan.asset.config.R2Properties
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Service for signing URLs for private assets
 * This generates signed URLs that will be verified by Cloudflare Worker
 */
@Service
class UrlSigningService(
    private val r2Properties: R2Properties,
) {
    private val algorithm = "HmacSHA256"

    /**
     * Generate signed URL for private asset access
     * @param path Storage path (e.g., "private/userId/assetId/filename.jpg")
     * @param expirationSeconds How long the URL is valid
     * @return Signed URL with expiration and signature
     */
    fun generateSignedUrl(
        path: String,
        expirationSeconds: Long = 3600,
    ): String {
        val expiresAt = Instant.now().plusSeconds(expirationSeconds).epochSecond

        // Create signature payload: path + expires
        val payload = "$path:$expiresAt"

        // Generate HMAC-SHA256 signature
        val signature = generateSignature(payload)

        // Build signed URL
        val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8)
        return "${r2Properties.cdnUrl}/$path?expires=$expiresAt&signature=$signature"
    }

    /**
     * Generate HMAC-SHA256 signature
     */
    private fun generateSignature(payload: String): String {
        val secretKeySpec = SecretKeySpec(r2Properties.signingKey.toByteArray(), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secretKeySpec)

        val signatureBytes = mac.doFinal(payload.toByteArray())
        return signatureBytes.joinToString("") { "%02x".format(it) }
    }
}
