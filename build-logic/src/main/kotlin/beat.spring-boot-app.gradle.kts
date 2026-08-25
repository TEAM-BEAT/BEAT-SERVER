import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.Exec
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
    id("beat.kotlin-base")
    id("beat.test")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

configurations.configureEach {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
}

dependencies {
    implementation(platform(libs.findLibrary("spring-boot-dependencies").get()))
    implementation(libs.findBundle("boot-app-core").get())
    implementation(libs.findLibrary("spring-boot-starter-log4j2").get())
    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    testImplementation(libs.findLibrary("spring-boot-starter-test").get())

    constraints {
        implementation(libs.findLibrary("commons-fileupload").get()) {
            because("Trivy reports CVE-2025-48976 against the OpenFeign form transitive 1.5 baseline")
        }
    }
}

val localDevSecretScript = rootDir.resolve("scripts/generate-local-dev-secret.sh")
val localVarsScript = rootDir.resolve("scripts/lib/local-vars.sh")
val localDevSecretSource = rootDir.resolve("infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml")
val localDevSecretOutput = rootDir.resolve("secret/application-dev-secret.properties")

fun localDevSecretHasRequiredKeys(): Boolean =
    localDevSecretOutput.exists() &&
        localDevSecretOutput.useLines { lines ->
            lines.any { it.startsWith("DB_HIKARI_MAX_POOL_SIZE=") }
        }

val prepareLocalDevSecret = tasks.register<Exec>("prepareLocalDevSecret") {
    description = "Generate the repo-local dev secret properties file used by bootRun."
    group = "application"
    commandLine(localDevSecretScript.absolutePath)
    inputs.files(localDevSecretScript, localVarsScript, localDevSecretSource)
    outputs.file(localDevSecretOutput)
    outputs.upToDateWhen { localDevSecretHasRequiredKeys() }
}

tasks.withType<BootRun>().configureEach {
    // Secret imports resolve from the shared repo-level `secret/` directory.
    workingDir = rootDir
    dependsOn(prepareLocalDevSecret)
}
