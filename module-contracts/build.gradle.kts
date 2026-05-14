plugins {
    id("beat.library")
    id("beat.sentry-source-context")
}

dependencies {
    api(project(":global-support"))
}
