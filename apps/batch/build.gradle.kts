plugins {
    id("beat.spring-boot-app")
    id("beat.actuator-http-runtime")
    id("beat.sentry-source-context")
    id("beat.prometheus-runtime")
}

base {
    archivesName.set("batch")
}

dependencies {
    implementation(project(":application:system"))
    implementation(project(":infrastructure"))
    implementation(project(":support:observability"))
    implementation(libs.kotlin.logging.jvm)
    runtimeOnly(libs.slf4j.api)

    testImplementation(libs.bundles.integration.testcontainers)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.kotest.extensions.spring)
    testImplementation(project(":domain"))
    testImplementation(libs.mockk)
}
