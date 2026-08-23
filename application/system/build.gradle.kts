plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":domain"))
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.kotlin.logging.jvm)
    // KotlinLogging은 SLF4J 퍼사드 — 백엔드 api는 직접 선언(전이 의존 아님)
    implementation(libs.slf4j.api)
}
