plugins {
    id("beat.spring-boot-app")
    id("beat.web-mvc")
    id("beat.web-security")
    id("beat.openapi")
    id("beat.feign-runtime")
    id("beat.sentry-source-context")
    id("beat.prometheus-runtime")
}

base {
    archivesName.set("apis")
}

dependencies {
    // kotlin-logging의 SLF4J 백엔드(전이 미제공)
    runtimeOnly(libs.slf4j.api)
    implementation(libs.kotlin.logging.jvm)
    // kotlin-logging의 SLF4J 백엔드(전이 미제공 — NoClassDefFoundError 실증)

    implementation(project(":application:frontoffice"))
    implementation(project(":support:security"))
    implementation(project(":infrastructure"))
    implementation(project(":support:observability"))
    runtimeOnly(libs.spring.boot.starter.data.redis)

    testImplementation(libs.bundles.integration.testcontainers)
    testImplementation(project(":domain"))
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.kotest.extensions.spring)
    // runtimeOnly(본체 런타임)와 별개로 테스트 '컴파일'에도 starter 클래스가 필요해 의도적 이중 선언
    testImplementation(libs.spring.boot.starter.data.redis)
    testImplementation(libs.spring.security.test)
}
