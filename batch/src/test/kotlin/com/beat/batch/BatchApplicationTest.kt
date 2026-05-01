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
        assertFalse(source.contains("TaskSchedulingAutoConfiguration::class"))
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
    fun `batch infra config keeps explicit base bootstrap groups`() {
        val configSource = Files.readString(Path.of("src/main/kotlin/com/beat/batch/config/InfraConfig.kt"))

        assertTrue(configSource.contains("InfraBaseConfigGroup.JPA"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.QUERY_DSL"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.ASYNC"))
        assertFalse(configSource.contains("InfraBaseConfigGroup.SCHEDULER"))
    }

    @Test
    fun `batch resources enable scheduler ownership by default`() {
        val config = Files.readString(Path.of("src/main/resources/application.yml"))

        assertTrue(config.contains("beat:"))
        assertTrue(config.contains("scheduler:"))
        assertTrue(config.contains("owner: true"))
        assertFalse(config.contains("owner: false"))
        assertTrue(config.contains("profiles:"))
        assertTrue(config.contains("group:"))
        assertTrue(config.contains("- persistence"))
        assertTrue(config.contains("- observability"))
        assertTrue(config.contains("- thread-pool"))
        val threadPoolConfig = Files.readString(Path.of("../infra/src/main/resources/application-thread-pool.yml"))
        assertTrue(threadPoolConfig.contains("spring:"))
        assertTrue(threadPoolConfig.contains("task:"))
        assertTrue(threadPoolConfig.contains("scheduling:"))
        assertTrue(threadPoolConfig.contains("thread-name-prefix: executor-scheduler-"))
        assertTrue(threadPoolConfig.contains("size: 2"))
        assertFalse(config.contains("- jwt"))
        assertFalse(config.contains("- redis"))
        assertFalse(config.contains("- external"))
        assertTrue(config.contains("on-profile: dev"))
        assertTrue(config.contains("application-dev-secret.properties"))
        assertTrue(config.contains("port: 4002"))
        assertTrue(config.contains("on-profile: prod"))
        assertTrue(config.contains("application-prod-secret.properties"))
        assertFalse(config.contains("BEAT_SERVER_PORT"))
        assertFalse(config.contains("management:"))
        assertFalse(config.contains("../secret/application-dev-secret.properties"))
        assertFalse(config.contains("../secret/application-prod-secret.properties"))
        assertFalse(config.contains("datasource:"))
    }

    @Test
    fun `batch actuator management config is owned by observability resource`() {
        val observabilityConfig = Files.readString(
            Path.of("../observability/src/main/resources/application-observability.yml"),
        )

        assertTrue(observabilityConfig.contains("port: \${DEV_ACTUATOR_PORT}"))
        assertTrue(observabilityConfig.contains("base-path: \${DEV_ACTUATOR_PATH}"))
        assertTrue(observabilityConfig.contains("port: \${PROD_ACTUATOR_PORT}"))
        assertTrue(observabilityConfig.contains("base-path: \${PROD_ACTUATOR_PATH}"))
        assertTrue(observabilityConfig.contains("base-path: /actuator-test"))
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
