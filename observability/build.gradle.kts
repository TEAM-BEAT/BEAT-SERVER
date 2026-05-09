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
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.kotlinx.coroutines.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}
