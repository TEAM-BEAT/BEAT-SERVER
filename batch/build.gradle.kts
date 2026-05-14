plugins {
    id("beat.spring-boot-app")
    id("beat.sentry-source-context")
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
