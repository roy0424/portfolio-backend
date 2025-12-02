dependencies {
    api(project(":common:common-core"))

    api(libs.jjwt.api)
    runtimeOnly(libs.bundles.jjwt.runtime)

    api(libs.spring.security.crypto)
    api(platform(libs.spring.boot.bom))
}
