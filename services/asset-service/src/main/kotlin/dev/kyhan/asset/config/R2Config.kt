package dev.kyhan.asset.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@ConfigurationProperties(prefix = "cloudflare.r2")
data class R2Properties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucketName: String,
    val region: String = "auto",
    val cdnUrl: String,
    val signingKey: String, // Secret key for signing private asset URLs
)

@Configuration
class R2Config {
    @Bean
    fun r2AsyncClient(r2Properties: R2Properties): S3AsyncClient {
        val credentials =
            AwsBasicCredentials.create(
                r2Properties.accessKey,
                r2Properties.secretKey,
            )

        return S3AsyncClient
            .builder()
            .endpointOverride(URI.create(r2Properties.endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(r2Properties.region))
            .serviceConfiguration(
                S3Configuration
                    .builder()
                    .pathStyleAccessEnabled(true) // R2 requires path-style access
                    .build(),
            ).build()
    }

    @Bean
    fun s3Presigner(r2Properties: R2Properties): S3Presigner {
        val credentials =
            AwsBasicCredentials.create(
                r2Properties.accessKey,
                r2Properties.secretKey,
            )

        return S3Presigner
            .builder()
            .endpointOverride(URI.create(r2Properties.endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(r2Properties.region))
            .serviceConfiguration(
                S3Configuration
                    .builder()
                    .pathStyleAccessEnabled(true)
                    .build(),
            ).build()
    }
}
