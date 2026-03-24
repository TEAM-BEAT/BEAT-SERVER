plugins {
    id("beat.spring-boot-app")
}

dependencies {
    implementation(project(":module-contracts"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation(project(":global-utils"))
    implementation(project(":observability"))

    testImplementation(libs.bundles.integration.testcontainers)
}
