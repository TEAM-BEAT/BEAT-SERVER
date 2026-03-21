plugins {
    id("beat.web-app")
}

dependencies {
    implementation(project(":"))
    implementation(project(":gateway"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation(project(":global-utils"))
    implementation(project(":observability"))
}
