plugins {
    id("beat.spring-library")
    id("beat.sentry-source-context")
}

dependencies {
    compileOnly(libs.spring.boot.starter.web)
    implementation(libs.kotlinx.coroutines.slf4j)
    implementation(libs.sentry.spring.boot.starter)
    runtimeOnly(libs.sentry.async.profiler)
    runtimeOnly(libs.sentry.log4j2)
    runtimeOnly(libs.log4j.layout.template.json)

    api(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.exporter.otlp)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
    testRuntimeOnly(libs.junit.platform.launcher)
}
