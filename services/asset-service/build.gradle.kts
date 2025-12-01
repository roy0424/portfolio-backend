plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common:common-core"))
    implementation(project(":common:common-security"))
    implementation(project(":common:common-event"))

    // Spring WebFlux
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // R2DBC for reactive PostgreSQL
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql")

    // AWS SDK for S3 (or Cloudflare R2)
    implementation(platform("software.amazon.awssdk:bom:2.29.41"))
    implementation("software.amazon.awssdk:s3")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
