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
    archivesName.set("admin")
}

dependencies {
    implementation(project(":application:admin"))
    implementation(project(":support:security"))
    implementation(project(":infrastructure"))
    implementation(project(":support:observability"))
    runtimeOnly(libs.slf4j.api)
    implementation(libs.kotlin.logging.jvm)

    testImplementation(project(":domain"))
    testImplementation(libs.bundles.integration.testcontainers)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.kotest.extensions.spring)
    testImplementation(libs.spring.security.test)
}
