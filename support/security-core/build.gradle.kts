plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(project(":application:frontoffice"))
    implementation(project(":support:observability"))
    implementation(libs.jjwt.api)
    implementation(libs.kotlin.logging.jvm)
    runtimeOnly(libs.slf4j.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.spring.boot.starter.validation)
    compileOnly(libs.spring.boot.starter.security)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
}
