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
    implementation(libs.kotlin.logging.jvm)

    implementation(project(":application:frontoffice"))
    implementation(project(":module-contracts"))
    implementation(project(":support:security"))
    implementation(project(":domain"))
    implementation(project(":infrastructure"))
    implementation(project(":global-support"))
    implementation(project(":support:observability"))
    runtimeOnly(libs.spring.boot.starter.data.redis)

    testImplementation(libs.bundles.integration.testcontainers)
    testImplementation(libs.spring.boot.starter.data.redis)
}
