package dev.kyhan.asset.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "asset")
data class AssetProperties(
    val maxFileSize: Long = 1073741824L, // 1GB
    val allowedContentTypes: List<String> = emptyList(), // Empty = allow all
    val uploadPathPrefix: String = "",
)
