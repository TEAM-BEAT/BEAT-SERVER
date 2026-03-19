import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.beat.buildlogic"

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val kotlinVersion = libsCatalog.findVersion("kotlin").get().requiredVersion
val springBootVersion = libsCatalog.findVersion("spring-boot").get().requiredVersion
val dependencyManagementVersion = libsCatalog.findVersion("dependency-management").get().requiredVersion

fun pluginMarker(group: String, artifact: String, version: String): String =
    "$group:$artifact:$version"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(pluginMarker("org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.jvm.gradle.plugin", kotlinVersion))
    implementation(pluginMarker("org.jetbrains.kotlin.plugin.spring", "org.jetbrains.kotlin.plugin.spring.gradle.plugin", kotlinVersion))
    implementation(pluginMarker("org.jetbrains.kotlin.plugin.jpa", "org.jetbrains.kotlin.plugin.jpa.gradle.plugin", kotlinVersion))
    implementation(pluginMarker("org.springframework.boot", "org.springframework.boot.gradle.plugin", springBootVersion))
    implementation(
        pluginMarker(
            "io.spring.dependency-management",
            "io.spring.dependency-management.gradle.plugin",
            dependencyManagementVersion,
        )
    )
}
