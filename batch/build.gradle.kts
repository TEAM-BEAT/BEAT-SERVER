plugins {
    id("beat.spring-boot-app")
}

dependencies {
    implementation(project(":module-contracts"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation(project(":global-support"))
    implementation(project(":observability"))
    implementation(libs.spring.boot.starter.web)

    testImplementation(libs.bundles.integration.testcontainers)
}
