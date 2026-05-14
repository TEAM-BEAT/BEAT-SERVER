plugins {
    id("beat.web-app")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(project(":module-contracts"))
    implementation(project(":gateway"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation(project(":global-support"))
    implementation(project(":observability"))
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(libs.bundles.integration.testcontainers)
}
