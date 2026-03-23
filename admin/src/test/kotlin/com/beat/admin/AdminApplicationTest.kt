package com.beat.admin

import com.beat.admin.config.AdminSecurityConfig
import com.beat.admin.config.InfraConfig
import com.beat.gateway.GatewayModuleConfig
import com.beat.observability.ObservabilityModuleConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.nio.file.Path

class AdminApplicationTest {

    @Test
    fun `admin application keeps detached module import contract`() {
        val importAnnotation = AdminApplication::class.java.getAnnotation(Import::class.java)
        assertNotNull(importAnnotation, "AdminApplication must declare @Import")
        val importedClassNames = importAnnotation!!.value.map { it.java.name }.toSet()

        assertEquals(
            setOf(
                GatewayModuleConfig::class.java.name,
                InfraConfig::class.java.name,
                ObservabilityModuleConfig::class.java.name,
            ),
            importedClassNames,
        )
    }

    @Test
    fun `gateway module imports auth bootstrap config`() {
        val componentScan = GatewayModuleConfig::class.java.getAnnotation(ComponentScan::class.java)

        assertNotNull(componentScan)
        assertTrue(componentScan.basePackages.contains("com.beat.gateway"))
    }

    @Test
    fun `admin security config exists for module owned route policy`() {
        val configuration = AdminSecurityConfig::class.java.getAnnotation(Configuration::class.java)

        assertNotNull(configuration)
    }

    @Test
    fun `admin application no longer owns broad component scan`() {
        val componentScan = AdminApplication::class.java.getAnnotation(ComponentScan::class.java)
        assertNull(componentScan)
    }

    @Test
    fun `admin application does not enable scheduling`() {
        val enableScheduling = AdminApplication::class.java.getAnnotation(EnableScheduling::class.java)
        assertNull(enableScheduling)
    }

    @Test
    fun `admin application no longer owns feign bootstrap scanning`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/beat/admin/AdminApplication.kt"))

        assertFalse(source.contains("@EnableFeignClients"))
        assertFalse(source.contains("\"com.beat.domain\""))
        assertFalse(source.contains("\"com.beat.global\""))
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
