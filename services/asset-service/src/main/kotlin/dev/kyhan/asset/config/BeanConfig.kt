package dev.kyhan.asset.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(R2Properties::class, AssetProperties::class)
class BeanConfig
