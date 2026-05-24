plugins {
    id("beat.library")
    id("beat.sentry-source-context")
}

dependencies {
    // Jackson 3 (tools.jackson.*) — Spring Boot 4.0 default ObjectMapper.
    // jackson-annotations remains under com.fasterxml.jackson.annotation per Jackson 3 migration rules.
    compileOnly(libs.jackson3.databind)
}
