plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    api(project(":support:observability"))
    implementation(libs.jjwt.api)
    implementation(libs.kotlin.logging.jvm)
    runtimeOnly(libs.slf4j.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.spring.boot.starter.validation)
    compileOnly(libs.spring.boot.starter.security)
    compileOnly(libs.spring.boot.starter.web)

    // CurrentMember의 springdoc @Parameter 메타어노테이션 지원(compileOnly: 런타임 미포함)
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security)
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
