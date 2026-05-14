plugins {
    id("beat.library")
    id("beat.test")
}

dependencies {
    api(project(":global-support"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
