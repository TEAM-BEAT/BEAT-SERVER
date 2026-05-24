plugins {
    id("beat.library")
    id("beat.sentry-source-context")
}

dependencies {
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.15.4")
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.15.4")
}
