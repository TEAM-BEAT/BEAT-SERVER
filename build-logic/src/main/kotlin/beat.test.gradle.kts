import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

plugins {}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("testImplementation", libs.findLibrary("kotest-runner-junit5").get())
    add("testImplementation", libs.findLibrary("kotest-assertions-core").get())
}

tasks.withType<Test>().configureEach {
    // JUnit Platform remains the common execution contract for legacy Jupiter and Kotest.
    useJUnitPlatform()
    systemProperty("beat.openapi.output.dir", layout.buildDirectory.dir("openapi").get().asFile.absolutePath)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("openapi")
    }
    systemProperty("kotest.tags.exclude", "openapi")
}

val testSourceSet = extensions.getByType<SourceSetContainer>().named("test")

fun registerRiskTestTask(
    name: String,
    description: String,
    includedTags: Set<String> = emptySet(),
    excludedTags: Set<String> = emptySet(),
) {
    tasks.register<Test>(name) {
        group = "verification"
        this.description = description
        testClassesDirs = testSourceSet.get().output.classesDirs
        classpath = testSourceSet.get().runtimeClasspath
        filter.isFailOnNoMatchingTests = false
        useJUnitPlatform {
            if (includedTags.isNotEmpty()) includeTags(*includedTags.toTypedArray())
            if (excludedTags.isNotEmpty()) excludeTags(*excludedTags.toTypedArray())
        }
        if (includedTags.isNotEmpty()) {
            systemProperty("kotest.tags.include", includedTags.joinToString(","))
        }
        if (excludedTags.isNotEmpty()) {
            systemProperty("kotest.tags.exclude", excludedTags.joinToString(","))
        }
    }
}

registerRiskTestTask(
    name = "fastTest",
    description = "Runs tests that do not require integration, correctness, or acceptance infrastructure.",
    excludedTags = setOf("integration", "correctness", "acceptance", "openapi"),
)
registerRiskTestTask(
    name = "integrationTest",
    description = "Runs tests tagged as integration tests.",
    includedTags = setOf("integration"),
)
registerRiskTestTask(
    name = "correctnessTest",
    description = "Runs tests tagged for transaction, locking, or concurrency correctness.",
    includedTags = setOf("correctness"),
)
registerRiskTestTask(
    name = "acceptanceTest",
    description = "Runs tests tagged as acceptance journeys.",
    includedTags = setOf("acceptance"),
)
registerRiskTestTask(
    name = "openApiTest",
    description = "Runs OpenAPI compatibility tests.",
    includedTags = setOf("openapi"),
)
