plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common:common-core"))
    implementation(project(":common:common-event"))

    // Spring Boot (no webflux needed, just consumer)
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.actuator)

    // MongoDB Reactive for analytics storage
    implementation(libs.spring.boot.starter.data.mongodb.reactive)

    // Kafka Consumer
    implementation(libs.spring.kafka)

    testImplementation(libs.spring.kafka.test)
}
