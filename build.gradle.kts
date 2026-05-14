import io.sentry.android.gradle.extensions.SentryPluginExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
    alias(libs.plugins.sentry.jvm) apply false
    id("beat.test")
}

group = "com"
version = "0.0.1-SNAPSHOT"

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val sentrySdkVersion = libsCatalog.findVersion("sentry").get().requiredVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    testImplementation(project(":domain"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Jar>("jar") {
    enabled = true
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
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
    filter {
        includeTestsMatching("com.beat.architecture.PromotionBoundaryTest")
        includeTestsMatching("com.beat.RootRetirementContractTest")
        includeTestsMatching("com.beat.SharedBoundaryContractTest")
    }
}

registerVerificationTask(
    "verifyModuleBootJars",
    "Builds boot jars for the current executable modules.",
    ":apis:bootJar",
    ":admin:bootJar",
    ":batch:bootJar",
)

subprojects {
    group = rootProject.group
    version = rootProject.version

    configurations.configureEach {
        resolutionStrategy.force("io.sentry:sentry:$sentrySdkVersion")
        resolutionStrategy.eachDependency {
            if (requested.group == "io.sentry") {
                useVersion(sentrySdkVersion)
                because("Keep Sentry SDK artifacts aligned with the Boot 4 observability contract")
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        pluginManager.apply("io.sentry.jvm.gradle")
        extensions.configure<SentryPluginExtension>("sentry") {
            includeSourceContext.set(true)
            autoUploadSourceContext.set(
                providers.environmentVariable("SENTRY_AUTH_TOKEN").map { it.isNotBlank() }.orElse(false),
            )
            org.set("beat-jo")
            projectName.set("java-spring-boot")
            authToken.set(providers.environmentVariable("SENTRY_AUTH_TOKEN").orElse(""))
            autoInstallation {
                enabled.set(false)
                sentryVersion.set(sentrySdkVersion)
            }
            telemetry.set(false)
        }
    }
}
