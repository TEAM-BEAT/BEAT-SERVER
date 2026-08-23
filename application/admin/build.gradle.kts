plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

group = "com.beat.application"

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":domain"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")

    testImplementation(libs.archunit.junit5)
}
