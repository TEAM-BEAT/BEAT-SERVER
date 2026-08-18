plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(project(":domain"))
}
