plugins {
    id("beat.spring-boot-app")
    id("beat.actuator-http-runtime")
    id("beat.sentry-source-context")
    id("beat.prometheus-runtime")
}

dependencies {
    implementation(project(":module-contracts"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation(project(":global-support"))
    implementation(project(":observability"))

    testImplementation(libs.bundles.integration.testcontainers)
}
