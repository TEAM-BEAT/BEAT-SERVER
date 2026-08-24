import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.autonomousapps.dependency-analysis")
}

// 모듈 좌표는 루트 좌표를 따른다(모듈별 중복 선언 금지).
group = rootProject.group.toString()
version = rootProject.version.toString()

// Policy: compile application modules with JDK 25, but emit JVM 25-compatible bytecode
// so the transition baseline can run on a Java 25 runtime while the build stack upgrades.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xemit-jvm-type-annotations")
    }
}
