plugins {
    id("beat.spring-library")
}

dependencies {
    implementation(project(":global-support"))
    compileOnly(libs.spring.boot.core)
    compileOnly(libs.spring.boot.starter.actuator)
    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.spring.aop)
    compileOnly(libs.spring.tx)
    compileOnly(libs.aspectjweaver)
    compileOnly(libs.slf4j.api)
    compileOnly("net.minidev:json-smart:2.5.2")
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
