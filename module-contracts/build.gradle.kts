plugins {
    id("beat.library")
    id("beat.sentry-source-context")
}

dependencies {
    compileOnly(project(":global-support"))
}
