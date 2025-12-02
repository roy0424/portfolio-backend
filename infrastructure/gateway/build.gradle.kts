plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common:common-security"))

    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.starter.gateway)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)

    // OpenAPI/Swagger for API aggregation
    implementation(libs.springdoc.openapi.webflux.ui)
}
