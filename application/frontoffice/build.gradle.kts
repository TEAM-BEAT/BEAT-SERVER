plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":domain"))
    implementation(project(":support:security"))
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.kotlin.logging.jvm)
    runtimeOnly(libs.slf4j.api)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
}
