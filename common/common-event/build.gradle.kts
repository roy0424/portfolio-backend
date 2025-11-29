dependencies {
    api(project(":common:common-core"))

    api("org.springframework.kafka:spring-kafka")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.1")
    }
}