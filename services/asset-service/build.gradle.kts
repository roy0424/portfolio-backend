plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common:common-core"))
    implementation(project(":common:common-security"))
    implementation(project(":common:common-event"))

    // Spring WebFlux
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    // R2DBC for reactive PostgreSQL
    implementation(libs.bundles.r2dbc.postgres)

    // AWS SDK for S3 (or Cloudflare R2)
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.sdk.s3)

    // Kafka
    implementation(libs.spring.kafka)
}
