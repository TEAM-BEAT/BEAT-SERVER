plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(project(":module-contracts"))
    implementation(project(":observability"))
    api(project(":global-support"))
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.spring.boot.starter.data.redis)
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)
    compileOnly(libs.spring.boot.starter.security)
    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security)
    testImplementation(libs.spring.boot.starter.web)
    testRuntimeOnly(libs.junit.platform.launcher)
}
