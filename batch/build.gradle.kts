plugins {
    id("beat.spring-boot-app")
}

dependencies {
    implementation(project(":"))
    implementation(project(":domain"))
    implementation(project(":infra"))
    implementation(project(":global-utils"))
    implementation(project(":observability"))
}
