plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":domain"))
    implementation(project(":support:security"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(libs.kotlin.logging.jvm)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")
    testRuntimeOnly(libs.junit.platform.launcher)
}
