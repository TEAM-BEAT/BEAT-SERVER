plugins {
    id("beat.library")
}

dependencies {
    compileOnly(project(":domain"))
    compileOnly(project(":global-utils"))
}
