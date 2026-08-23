plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

group = "com.beat.application"

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":domain"))
    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    testImplementation(libs.archunit.junit5)
}
