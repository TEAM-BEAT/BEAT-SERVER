plugins {
    id("beat.spring-boot-app")
    id("beat.web-mvc")
    id("beat.web-security")
    id("beat.openapi")
    id("beat.feign-runtime")
    id("beat.sentry-source-context")
    id("beat.prometheus-runtime")
}

base {
    archivesName.set("apis")
}

dependencies {
    implementation(libs.kotlin.logging.jvm)

    implementation(project(":application:frontoffice"))
    implementation(project(":support:security"))
    implementation(project(":infrastructure"))
    implementation(project(":support:observability"))
    runtimeOnly(libs.spring.boot.starter.data.redis)

    testImplementation(libs.bundles.integration.testcontainers)
    testImplementation(project(":domain"))
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.kotest.extensions.spring)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.starter.data.redis)
}
