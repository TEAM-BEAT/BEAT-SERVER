plugins {
    id("beat.spring-library")
}

val queryDslSrcDir = layout.buildDirectory.dir("generated/querydsl")

dependencies {
    api(project(":global-utils"))
    implementation(kotlin("reflect"))
    compileOnly(libs.spring.boot.starter.data.jpa)
    compileOnly(libs.spring.boot.starter.security)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // QueryDSL APT — entity Q-type generation
    compileOnly(libs.querydsl.jpa.jakarta) {
        artifact { classifier = "jakarta" }
    }
    annotationProcessor(libs.querydsl.apt.jakarta) {
        artifact { classifier = "jakarta" }
    }
    annotationProcessor(libs.jakarta.annotation.api)
    annotationProcessor(libs.jakarta.persistence.api)
}

tasks.withType<JavaCompile>().configureEach {
    options.generatedSourceOutputDirectory.set(queryDslSrcDir.get().asFile)
}

sourceSets {
    named("main") {
        java.srcDir(queryDslSrcDir)
    }
}
