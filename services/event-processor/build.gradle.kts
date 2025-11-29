plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common:common-core"))
    implementation(project(":common:common-event"))

    // Spring Boot (no webflux needed, just consumer)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // MongoDB Reactive for analytics storage
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

    // Kafka Consumer
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}