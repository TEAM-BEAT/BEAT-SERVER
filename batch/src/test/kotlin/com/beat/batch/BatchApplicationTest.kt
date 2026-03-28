package com.beat.batch

import com.beat.batch.config.InfraConfig
import com.beat.observability.ObservabilityModuleConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.nio.file.Path

class BatchApplicationTest {

    @Test
    fun `batch application keeps detached module import contract`() {
        val importAnnotation = BatchApplication::class.java.getAnnotation(Import::class.java)
        assertNotNull(importAnnotation, "BatchApplication must declare @Import")
        val importedClassNames = importAnnotation!!.value.map { it.java.name }.toSet()

        assertEquals(
            setOf(
                InfraConfig::class.java.name,
                ObservabilityModuleConfig::class.java.name,
            ),
            importedClassNames,
        )
    }

    @Test
    fun `batch application keeps scheduling local to the module bootstrap`() {
        val springBootApplication = BatchApplication::class.java.getAnnotation(SpringBootApplication::class.java)
        val componentScan = BatchApplication::class.java.getAnnotation(ComponentScan::class.java)
        val enableScheduling = BatchApplication::class.java.getAnnotation(EnableScheduling::class.java)
        val source = Files.readString(Path.of("src/main/kotlin/com/beat/batch/BatchApplication.kt"))

        assertNotNull(springBootApplication)
        assertNull(componentScan)
        assertNotNull(enableScheduling)
        assertEquals(
            setOf(BatchApplication::class.java.name),
            springBootApplication!!.scanBasePackageClasses.map { it.java.name }.toSet(),
        )
        assertFalse(source.contains("BatchSchedulerBootstrapConfig"))
        assertFalse(source.contains("GatewayModuleConfig"))
        assertFalse(source.contains("@EnableFeignClients"))
        assertFalse(source.contains("FeignAutoConfiguration"))
        assertTrue(source.contains("TaskSchedulingAutoConfiguration::class"))
        assertFalse(source.contains("\"com.beat.domain\""))
        assertFalse(source.contains("\"com.beat.global\""))
    }

    @Test
    fun `batch owner sources no longer declare legacy owner packages`() {
        val paths = Files.walk(Path.of("src/main"))
        val violations = try {
            paths
                .filter(Files::isRegularFile)
                .filter { it.toString().endsWith(".java") || it.toString().endsWith(".kt") }
                .filter { path ->
                    val source = Files.readString(path)
                    source.startsWith("package com.beat.domain.")
                        || source.startsWith("package com.beat.global.")
                }
                .map(Path::toString)
                .toList()
        } finally {
            paths.close()
        }

        assertTrue(violations.isEmpty(), "Found legacy owner package declarations:\n${violations.joinToString("\n")}")
    }

    @Test
    fun `batch infra config keeps async executor and scheduler imports explicit`() {
        val configSource = Files.readString(Path.of("src/main/kotlin/com/beat/batch/config/InfraConfig.kt"))

        assertTrue(configSource.contains("InfraBaseConfigGroup.JPA"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.QUERY_DSL"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.ASYNC"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.SCHEDULER"))
    }

    @Test
    fun `batch resources enable scheduler ownership by default`() {
        val config = Files.readString(Path.of("src/main/resources/application.yml"))
        assertTrue(config.contains("beat:"))
        assertTrue(config.contains("scheduler:"))
        assertTrue(config.contains("owner: true"))
        assertFalse(config.contains("owner: false"))
    }

    @Test
    fun `batch test resources disable scheduler ownership for smoke tests`() {
        val config = Files.readString(Path.of("src/test/resources/application-test.yml"))

        assertTrue(config.contains("beat:"))
        assertTrue(config.contains("scheduler:"))
        assertTrue(config.contains("owner: false"))
        assertFalse(config.contains("owner: true"))
        assertFalse(config.contains("allow-bean-definition-overriding"))
    }
}
