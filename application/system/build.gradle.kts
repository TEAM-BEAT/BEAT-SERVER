plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":domain"))
    implementation("org.slf4j:slf4j-api")
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
}
