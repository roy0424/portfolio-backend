plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover)
}

allprojects {
    group = "dev.kyhan"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

dependencies {
    kover(project(":common:common-core"))
    kover(project(":common:common-security"))
    kover(project(":common:common-event"))
    kover(project(":services:auth-service"))
    kover(project(":infrastructure:gateway"))
}

subprojects {
    if (name in listOf("common", "services", "infrastructure")) {
        return@subprojects
    }

    pluginManager.apply(rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    pluginManager.apply(rootProject.libs.plugins.kotlin.spring.get().pluginId)
    pluginManager.apply(rootProject.libs.plugins.spring.dependency.management.get().pluginId)
    pluginManager.apply(rootProject.libs.plugins.ktlint.get().pluginId)
    pluginManager.apply(rootProject.libs.plugins.kover.get().pluginId)

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    dependencies {
        "implementation"(rootProject.libs.kotlin.reflect)
        "implementation"(rootProject.libs.kotlin.stdlib.jdk8)
        "implementation"(rootProject.libs.kotlin.logging.jvm)

        "testImplementation"(rootProject.libs.spring.boot.starter.test)
        "testImplementation"(rootProject.libs.kotlin.test.junit5)
        "testImplementation"(rootProject.libs.mockk)
        "testImplementation"(rootProject.libs.springmockk)
        "testImplementation"(rootProject.libs.reactor.test)
    }


    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
