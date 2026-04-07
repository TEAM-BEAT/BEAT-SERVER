plugins {
    id("beat.web-app")
}

dependencies {
    implementation(project(":module-contracts"))
    implementation(project(":gateway"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation(project(":global-utils"))
    implementation(project(":observability"))
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(libs.bundles.integration.testcontainers)
}
