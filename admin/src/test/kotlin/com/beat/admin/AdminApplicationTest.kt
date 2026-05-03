package com.beat.admin

import com.beat.admin.config.AdminSecurityConfig
import com.beat.admin.config.InfraConfig
import com.beat.gateway.EnableGatewayConfig
import com.beat.gateway.GatewayConfigGroup
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
                InfraConfig::class.java.name,
                ObservabilityModuleConfig::class.java.name,
            ),
            importedClassNames,
        )
    }

    @Test
    fun `admin selects gateway servlet security without refresh token store`() {
        val enableGatewayConfig = AdminApplication::class.java.getAnnotation(EnableGatewayConfig::class.java)

        assertNotNull(enableGatewayConfig, "AdminApplication must declare @EnableGatewayConfig")
        assertEquals(
            setOf(GatewayConfigGroup.SERVLET_SECURITY),
            enableGatewayConfig!!.value.toSet(),
        )
    }

    @Test
    fun `gateway module exposes selector based public bootstrap without broad gateway scan`() {
        val componentScan = GatewayModuleConfig::class.java.getAnnotation(ComponentScan::class.java)
        val broadGatewayScans = componentScan
            ?.basePackages
            ?.filter { it == "com.beat.gateway" || it == "com.beat.gateway.*" }
            .orEmpty()
        val gatewayModuleSourcePath = listOf(
            Path.of("../gateway/src/main/kotlin/com/beat/gateway/GatewayModuleConfig.kt"),
            Path.of("gateway/src/main/kotlin/com/beat/gateway/GatewayModuleConfig.kt"),
        ).first(Files::exists)
        val gatewayModuleSource = Files.readString(gatewayModuleSourcePath)

        assertTrue(broadGatewayScans.isEmpty(), "GatewayModuleConfig must not broad-scan com.beat.gateway")
        assertFalse(gatewayModuleSource.contains("basePackages = [\"com.beat.gateway\"]"))
        assertTrue(
            gatewayModuleSource.contains("@EnableGatewayConfig"),
            "GatewayModuleConfig must delegate to gateway selector config groups",
        )
    }

    @Test
    fun `admin security config exists for module owned route policy`() {
        val configuration = AdminSecurityConfig::class.java.getAnnotation(Configuration::class.java)

        assertNotNull(configuration)
    }

    @Test
    fun `admin swagger config exists as non prod module owned documentation policy`() {
        val source = Files.readString(Path.of("src/main/java/com/beat/admin/swagger/config/AdminSwaggerConfig.java"))
        val securitySource = Files.readString(Path.of("src/main/java/com/beat/admin/config/AdminSecurityConfig.java"))

        assertTrue(source.contains("@Profile(\"!prod\")"))
        assertTrue(source.contains(".group(\"admin\")"))
        assertTrue(source.contains("pathsToMatch(\"/api/admin/**\")"))
        assertTrue(securitySource.contains("if (!environment.acceptsProfiles(Profiles.of(\"prod\")))"))
        assertTrue(securitySource.contains("Collections.addAll(whitelist, SWAGGER_WHITELIST)"))
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
    fun `admin infra config excludes async and scheduler transitional imports`() {
        val configSource = Files.readString(Path.of("src/main/kotlin/com/beat/admin/config/InfraConfig.kt"))

        assertTrue(configSource.contains("InfraBaseConfigGroup.JPA"))
        assertFalse(configSource.contains("InfraBaseConfigGroup.QUERY_DSL"))
        assertFalse(configSource.contains("InfraBaseConfigGroup.REDIS"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.EXTERNAL_CLIENTS"))
        assertFalse(configSource.contains("InfraBaseConfigGroup.ASYNC"))
        assertFalse(configSource.contains("InfraBaseConfigGroup.SCHEDULER"))
    }

    @Test
    fun `admin resources keep scheduler owner disabled`() {
        val config = Files.readString(Path.of("src/main/resources/application.yml"))

        assertTrue(config.contains("beat:"))
        assertTrue(config.contains("scheduler:"))
        assertTrue(config.contains("owner: false"))
        assertFalse(config.contains("owner: true"))
        assertTrue(config.contains("profiles:"))
        assertTrue(config.contains("group:"))
        assertTrue(config.contains("- persistence"))
        assertTrue(config.contains("- jwt"))
        assertTrue(config.contains("application-dev-secret.properties"))
        assertTrue(config.contains("application-prod-secret.properties"))
        assertTrue(config.contains("port: 4000"))
        assertFalse(config.contains("BEAT_SERVER_PORT"))
        assertFalse(config.contains("management:"))
        assertFalse(config.contains("../secret/application-dev-secret.properties"))
        assertFalse(config.contains("../secret/application-prod-secret.properties"))
    }

    @Test
    fun `admin module boot test uses targeted mocks without blanket bean overriding`() {
        val config = Files.readString(Path.of("src/test/resources/application-test.yml"))
        val bootTest = Files.readString(Path.of("src/test/java/com/beat/admin/AdminModuleContextBootTest.java"))

        assertFalse(config.contains("allow-bean-definition-overriding"))
        assertTrue(bootTest.contains("@MockitoBean"))
        assertTrue(bootTest.contains("FileStoragePort"))
        assertFalse(bootTest.contains("PromotionUseCase promotionUseCase"))
        assertFalse(bootTest.contains("PerformanceUseCase performanceUseCase"))
        assertFalse(bootTest.contains("MemberUseCase memberUseCase"))
        assertFalse(bootTest.contains("UserUseCase userUseCase"))
        assertFalse(bootTest.contains("AdminUseCase adminUseCase"))
        assertFalse(bootTest.contains("allow-bean-definition-overriding"))
    }
}
