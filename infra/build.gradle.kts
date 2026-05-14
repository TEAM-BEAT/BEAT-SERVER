plugins {
    id("beat.jpa-adapter")
    id("beat.external-client")
    id("beat.sentry-source-context")
}

dependencies {
    implementation(platform(libs.aws.java.sdk.bom))
    implementation(project(":module-contracts"))
    implementation(project(":domain"))
    implementation(project(":global-support"))
    implementation(libs.aws.java.sdk.s3)
    implementation(libs.nurigo.java.sdk) {
        // javaSDK 2.2 incorrectly declares Maven build plugins as runtime dependencies.
        // They are not needed by CoolSmsAdapter and pull vulnerable Struts/Maven tooling
        // into executable boot jars.
        exclude(group = "org.apache.maven.plugins")
    }
    runtimeOnly(libs.mysql.connector.j)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
