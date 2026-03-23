plugins {
    id("beat.spring-library")
}

dependencies {
    implementation(project(":module-contracts"))
    api(project(":global-utils"))
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.spring.boot.starter.data.redis)
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)
    compileOnly(libs.spring.boot.starter.security)
    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.spring.boot.core)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
