plugins {
    id("beat.library")
    id("beat.test")
    id("beat.sentry-source-context")
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
