plugins {
    id("beat.spring-library")
}

dependencies {
    implementation(project(":global-support"))
    compileOnly(libs.spring.boot.core)
    compileOnly(libs.spring.boot.starter.actuator)
    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.slf4j.api)
    implementation(libs.kotlinx.coroutines.slf4j)
    implementation(libs.sentry.spring.boot.starter)
    implementation(libs.sentry.async.profiler)
    implementation(libs.sentry.log4j2)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.kotlinx.coroutines.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}
