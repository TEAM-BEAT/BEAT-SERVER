plugins {
    id("beat.library")
    id("beat.test")
    id("beat.sentry-source-context")
}

dependencies {
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
