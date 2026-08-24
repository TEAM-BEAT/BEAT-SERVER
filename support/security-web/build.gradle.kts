plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(project(":support:security"))
    implementation(project(":support:observability"))
    implementation(libs.kotlin.logging.jvm)
    runtimeOnly(libs.slf4j.api)
    implementation(libs.spring.boot.starter.validation)
    compileOnly(libs.spring.boot.starter.security)
    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security)
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
}
