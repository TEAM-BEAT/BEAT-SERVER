plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":domain"))
    implementation(project(":module-contracts"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
