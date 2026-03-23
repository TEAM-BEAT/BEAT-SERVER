plugins {
    id("beat.web-app")
}

dependencies {
    implementation(project(":module-contracts"))
    implementation(project(":"))
    implementation(project(":gateway"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation(project(":global-utils"))
    implementation(project(":observability"))

    testImplementation(libs.bundles.integration.testcontainers)
}
