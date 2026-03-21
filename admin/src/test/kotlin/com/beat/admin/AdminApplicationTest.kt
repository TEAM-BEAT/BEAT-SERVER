package com.beat.admin

import com.beat.admin.config.InfraConfig
import com.beat.gateway.GatewayModuleConfig
import com.beat.observability.ObservabilityModuleConfig
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

class AdminApplicationTest {

    @Test
    fun `admin application keeps transition baseline module imports`() {
        val importAnnotation = AdminApplication::class.java.getAnnotation(Import::class.java)
        val importedClassNames = importAnnotation.value.map { it.java.name }.toSet()

        assertTrue(importedClassNames.contains(GatewayModuleConfig::class.java.name))
        assertTrue(importedClassNames.contains(InfraConfig::class.java.name))
        assertTrue(importedClassNames.contains(ObservabilityModuleConfig::class.java.name))
    }

    @Test
    fun `admin application does not enable scheduling`() {
        val enableScheduling = AdminApplication::class.java.getAnnotation(EnableScheduling::class.java)
        assertNull(enableScheduling)
    }

    @Test
    fun `admin resources keep scheduler owner disabled`() {
        val config = Files.readString(Path.of("src/main/resources/application.yml"))

        assertTrue(config.contains("beat:"))
        assertTrue(config.contains("scheduler:"))
        assertTrue(config.contains("owner: false"))
        assertFalse(config.contains("owner: true"))
    }
}
