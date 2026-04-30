plugins {
    id("beat.library")
}

dependencies {
    api(project(":global-utils"))
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
