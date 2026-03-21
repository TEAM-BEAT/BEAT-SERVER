plugins {
    id("beat.jpa-infra")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":global-utils"))
}
