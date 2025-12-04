plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common:common-security"))
    implementation(project(":common:common-event"))
    implementation(project(":common:common-grpc"))

    // Spring WebFlux
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    // grpc client
    implementation(libs.grpc.client.spring.boot.starter)

    // R2DBC for reactive PostgreSQL
    implementation(libs.bundles.r2dbc.postgres)

    // Flyway for DB migration
    implementation(libs.bundles.flyway.postgres)

    // Security & OAuth2
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)

    // Kafka
    implementation(libs.spring.kafka)

    // Redis
    implementation(libs.spring.boot.starter.data.redis.reactive)
    implementation(libs.lettuce.core)

    // Email sending
    implementation(libs.spring.boot.starter.mail)

    // OpenAPI/Swagger
    implementation(libs.springdoc.openapi.webflux.ui)
}
