plugins {
    id("beat.spring-library")
}

dependencies {
    api(project(":global-utils"))
    compileOnly(libs.spring.boot.core)
}
