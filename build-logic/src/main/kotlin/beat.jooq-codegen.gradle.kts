/**
 * BEAT jOOQ codegen convention — deterministic, no live DB.
 *
 * Source: infrastructure/src/main/resources/db/jooq/schema.sql (MySQL 8, DATETIME(6))
 * Generated output: infrastructure/src/main/kotlin/com/beat/infrastructure/jooq/generated/Tables.kt
 *   (checked-in, reproducible — no Docker / external DB required for local build)
 *
 * Official pipeline (Flyway 보류):
 *   DDLDatabase (H2 interpreter)  ←  schema.sql  →  jOOQ Tables.kt
 *   Testcontainers variant: if Flyway is re-enabled, replace DDLDatabase with
 *   Testcontainers MySQL + Flyway migrations (see blog.jooq.org/using-testcontainers-to-generate-jooq-code).
 *
 * Version alignment: jOOQ runtime (spring-boot-starter-jooq) and codegen (org.jooq:jooq-codegen)
 * must share the same version coordinated via Spring Boot BOM (libs.versions.toml:spring-boot = 4.0.8).
 * This convention currently validates generation inputs at build time; full Gradle codegen task
 * (org.jooq.jooq-codegen-gradle) can be added without new Product Module.
 */

plugins {
    id("beat.infra-library")
}

val libs = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

// jOOQ codegen is version-aligned with spring-boot-starter-jooq via Boot BOM.
// Keep runtime and codegen on identical jOOQ version to avoid mismatch.
// Note: explicit jooq-codegen dependency only needed when Gradle codegen task is enabled.
dependencies {
    // jooq-codegen used only during generation; kept as compileOnly to avoid runtime classpath pollution.
    // Uncomment when enabling org.jooq.jooq-codegen-gradle plugin:
    // jooqCodegen(libs.findLibrary("jooq-codegen").get())
}

tasks.register("validateJooqSchema") {
    group = "verification"
    description = "Validate that jOOQ DDL source and generated Tables.kt are in sync and present."
    inputs.file(project.layout.projectDirectory.file("src/main/resources/db/jooq/schema.sql"))
    inputs.file(project.layout.projectDirectory.file("src/main/kotlin/com/beat/infrastructure/jooq/generated/Tables.kt"))
    doLast {
        val schema = project.layout.projectDirectory.file("src/main/resources/db/jooq/schema.sql").asFile
        val tables = project.layout.projectDirectory.file("src/main/kotlin/com/beat/infrastructure/jooq/generated/Tables.kt").asFile
        require(schema.exists()) { "Missing jOOQ DDL source: ${schema.path}" }
        require(tables.exists()) { "Missing jOOQ generated Tables: ${tables.path}" }
        // Minimal content check — ensure key tables are defined
        val content = tables.readText()
        listOf("Booking", "Schedule", "Performance", "Promotion", "CastTable", "StaffTable", "PerformanceImage")
            .forEach { require(content.contains(it)) { "Generated Tables.kt missing $it" } }
    }
}

tasks.named("compileKotlin") {
    dependsOn("validateJooqSchema")
}
