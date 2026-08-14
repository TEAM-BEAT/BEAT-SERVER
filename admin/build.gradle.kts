plugins {
    id("beat.spring-boot-app")
    id("beat.web-mvc")
    id("beat.web-security")
    id("beat.openapi")
    id("beat.feign-runtime")
    id("beat.sentry-source-context")
    id("beat.prometheus-runtime")
}

dependencies {
    implementation(project(":module-contracts"))
    implementation(project(":gateway"))
    implementation(project(":core:domain"))
    implementation(project(":core:infra"))
    implementation(project(":global-support"))
    implementation(project(":observability"))

    testImplementation(libs.bundles.integration.testcontainers)
}
