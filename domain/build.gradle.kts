plugins {
    id("beat.library")
    id("beat.test")
    id("beat.sentry-source-context")
}

dependencies {
    api(project(":global-support"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
