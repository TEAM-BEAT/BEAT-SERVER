plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":domain"))
    implementation(project(":support:security"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
