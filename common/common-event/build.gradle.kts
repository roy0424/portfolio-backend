dependencies {
    api(project(":common:common-core"))

    api(libs.spring.kafka)
    api(libs.jackson.module.kotlin)
    api(platform(libs.spring.boot.bom))
}
