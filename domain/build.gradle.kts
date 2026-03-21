plugins {
    id("beat.spring-library")
}

dependencies {
    api(project(":global-utils"))
    implementation(kotlin("reflect"))
}
