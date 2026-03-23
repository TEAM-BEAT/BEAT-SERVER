import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)

    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
    id("beat.test")
}

group = "com"
version = "0.0.1-SNAPSHOT"

val queryDslSrcDir = layout.buildDirectory.dir("generated/querydsl")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }

    configureEach {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

dependencies {
    implementation(project(":module-contracts"))
    implementation(project(":global-utils"))
    implementation(project(":gateway"))
    implementation(project(":observability"))
    implementation(project(":infra"))
    implementation(project(":domain"))

    // Web and security
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.starter.data.redis)

    // Legacy root runtime support
    implementation(libs.spring.aop)
    runtimeOnly(libs.aspectjweaver)
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    runtimeOnly(libs.mysql.connector.j)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Cloud and integration
    implementation(platform(libs.spring.cloud.dependencies))
    implementation(libs.spring.cloud.starter.openfeign)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(platform(libs.awspring.dependencies))
    implementation(libs.awspring.starter)
    implementation(libs.awspring.secrets.manager)

    // Legacy AWS SDK v1 support remains only while S3 code still uses com.amazonaws.* types.
    implementation(platform(libs.aws.java.sdk.bom))
    implementation(libs.aws.java.sdk.s3)
    implementation(libs.nurigo.sdk)
    implementation(libs.nurigo.java.sdk)

    // QueryDSL APT still relies on javac annotation processing for generated Q-types.
    implementation(libs.querydsl.jpa.jakarta) {
        artifact {
            classifier = "jakarta"
        }
    }
    annotationProcessor(libs.querydsl.apt.jakarta) {
        artifact {
            classifier = "jakarta"
        }
    }
    annotationProcessor(libs.jakarta.annotation.api)
    annotationProcessor(libs.jakarta.persistence.api)

    implementation(libs.kotlin.jdsl.jpql.dsl)
    implementation(libs.kotlin.jdsl.spring.data.jpa.support)

    // Observability
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.spring.boot.starter.log4j2)
    implementation(libs.slack.api.client)

    // Test support
    testImplementation(libs.bundles.test.common)
    testImplementation(libs.bundles.integration.testcontainers)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Jar>("jar") {
    enabled = true
}

tasks.withType<JavaCompile>().configureEach {
    options.generatedSourceOutputDirectory.set(queryDslSrcDir.get().asFile)
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
        java.srcDir(queryDslSrcDir)
    }
}

fun registerVerificationTask(
    name: String,
    description: String,
    vararg dependencies: Any,
) {
    tasks.register(name) {
        group = "verification"
        this.description = description
        dependsOn(*dependencies)
    }
}

val transitionBoundaryTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the root transition boundary guard tests only."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("com.beat.architecture.PromotionBoundaryTest") }
}

registerVerificationTask(
    "verifyLegacyV1Baseline",
    "Builds the legacy v1 root boot jar without coupling to the v2-web baseline.",
    "bootJar",
)
registerVerificationTask(
    "verifyV2WebBaseline",
    "Verifies the v2-web transition baseline with module tests and the root boundary guard.",
    ":apis:test",
    ":apis:bootJar",
    transitionBoundaryTest,
)
registerVerificationTask(
    "verifyModuleBootJars",
    "Builds boot jars for the current executable modules.",
    ":apis:bootJar",
    ":admin:bootJar",
    ":batch:bootJar",
)

tasks.clean {
    delete(queryDslSrcDir)
}

subprojects {
    group = rootProject.group
    version = rootProject.version
}
