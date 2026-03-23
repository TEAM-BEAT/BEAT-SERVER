plugins {
    id("beat.jpa-infra")
}

dependencies {
    compileOnly(platform(libs.spring.cloud.dependencies))
    compileOnly(platform(libs.aws.java.sdk.bom))
    implementation(project(":module-contracts"))
    implementation(project(":domain"))
    implementation(project(":global-utils"))
    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.spring.cloud.starter.openfeign)
    compileOnly(libs.aws.java.sdk.s3)
    compileOnly(libs.nurigo.java.sdk)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
