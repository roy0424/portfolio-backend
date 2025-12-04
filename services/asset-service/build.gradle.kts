plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common:common-event"))
    implementation(project(":common:common-grpc"))

    // Spring WebFlux
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    // grpc server
    implementation(libs.grpc.server.spring.boot.starter)
    runtimeOnly(libs.grpc.netty)

    // R2DBC for reactive PostgreSQL
    implementation(libs.bundles.r2dbc.postgres)

    // Flyway for DB migration
    implementation(libs.bundles.flyway.postgres)

    // AWS SDK for S3 (or Cloudflare R2)
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.sdk.s3)

    // Kafka
    implementation(libs.spring.kafka)

    // OpenAPI/Swagger
    implementation(libs.springdoc.openapi.webflux.ui)

    // Test dependencies
    testImplementation(libs.kotlinx.coroutines.test)
}
