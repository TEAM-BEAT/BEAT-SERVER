plugins {
    id("beat.library")
    id("beat.test")
    id("beat.sentry-source-context")
}

dependencies {
    testRuntimeOnly(libs.junit.platform.launcher)
}
