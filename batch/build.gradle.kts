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
    implementation(project(":module-contracts"))
    implementation(project(":domain"))
    implementation(project(":infrastructure"))
    implementation(project(":global-support"))
    implementation(project(":support:observability"))

    testImplementation(libs.bundles.integration.testcontainers)
}
