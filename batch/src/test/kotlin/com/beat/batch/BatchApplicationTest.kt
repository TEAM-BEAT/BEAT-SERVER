package com.beat.batch

import com.beat.batch.config.InfraConfig
import com.beat.observability.ObservabilityModuleConfig
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

class BatchApplicationTest {

    @Test
    fun `batch application keeps transition baseline scheduling contract`() {
        val importAnnotation = BatchApplication::class.java.getAnnotation(Import::class.java)
        val importedClassNames = importAnnotation.value.map { it.java.name }.toSet()
        val componentScan = BatchApplication::class.java.getAnnotation(ComponentScan::class.java)
        val enableScheduling = BatchApplication::class.java.getAnnotation(EnableScheduling::class.java)

        assertTrue(importedClassNames.contains(InfraConfig::class.java.name))
        assertTrue(importedClassNames.contains(ObservabilityModuleConfig::class.java.name))
        assertFalse(importedClassNames.contains("com.beat.gateway.GatewayModuleConfig"))
        assertTrue(componentScan.basePackages.contains("com.beat.global.common.scheduler"))
        assertTrue(enableScheduling != null)
    }

    @Test
    fun `batch resources keep scheduler ownership disabled during transition`() {
        val config = Files.readString(Path.of("src/main/resources/application.yml"))
        assertTrue(config.contains("beat:"))
        assertTrue(config.contains("scheduler:"))
        assertTrue(config.contains("owner: false"))
        assertFalse(config.contains("owner: true"))
    }
}
