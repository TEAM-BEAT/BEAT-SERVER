import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    kotlin("plugin.jpa") version "2.2.20"

    id("org.sonarqube") version "7.2.3.7755"
    id("org.jetbrains.kotlinx.kover") version "0.9.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"

val queryDslVersion = "5.0.0"
val springCloudVersion = "2025.1.1"
val springdocVersion = "3.0.2"
val awspringVersion = "4.0.0"
val jjwtVersion = "0.13.0"
val kotlinJdslVersion = "3.6.0"
val awsSdkV1Version = "1.12.797" // TEMP: legacy com.amazonaws.* S3 code compile shim

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }

    create("querydsl") {
        extendsFrom(configurations.compileClasspath.get())
    }

    configureEach {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

repositories {
    mavenCentral()
    maven("https://repo.spring.io/milestone")
    maven("https://repo.spring.io/snapshot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("org.springframework:spring-aop")
    runtimeOnly("org.aspectj:aspectjweaver")

    implementation(kotlin("reflect"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    runtimeOnly("com.mysql:mysql-connector-j")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:$awspringVersion"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager")

    // TEMP: legacy AWS SDK v1 compile compatibility
    // Current S3 code uses com.amazonaws.* (AmazonS3, BasicAWSCredentials, GeneratePresignedUrlRequest).
    // Keep this only until S3Config/FileService are migrated to AWS SDK v2 (S3Client/S3Presigner).
    implementation(platform("com.amazonaws:aws-java-sdk-bom:$awsSdkV1Version"))
    implementation("com.amazonaws:aws-java-sdk-s3")

    implementation("net.nurigo:sdk:4.3.0")
    implementation("net.nurigo:javaSDK:2.2")

    // NOTE:
    // QueryDSL APT is configured via javac annotationProcessor only.
    // Until QueryDSL-based queries are migrated away from Q-types
    // (e.g. Kotlin JDSL / jOOQ path), entities requiring Q-type generation
    // must remain in Java sources.
    implementation("com.querydsl:querydsl-jpa:$queryDslVersion:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:$kotlinJdslVersion")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("com.slack.api:slack-api-client:1.45.3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

val queryDslSrcDir = "src/main/generated/querydsl"

tasks.withType<JavaCompile>().configureEach {
    options.generatedSourceOutputDirectory.set(file(queryDslSrcDir))
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

sourceSets {
    named("main") {
        java.srcDirs(queryDslSrcDir)
    }
}

tasks.clean {
    delete(file(queryDslSrcDir))
}