plugins {
    id("beat.spring-library")
}

dependencies {
    implementation(project(":global-utils"))
    compileOnly(libs.spring.boot.core)
}
