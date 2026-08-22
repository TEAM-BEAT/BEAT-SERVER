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

    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")
}
