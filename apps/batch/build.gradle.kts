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

    testImplementation(libs.bundles.integration.testcontainers)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")
    testImplementation(libs.kotest.extensions.spring)
    testImplementation(project(":domain"))
}
