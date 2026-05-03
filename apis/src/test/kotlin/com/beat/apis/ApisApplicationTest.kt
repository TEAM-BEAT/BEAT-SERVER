package com.beat.apis

import com.beat.apis.config.ApisSecurityConfig
import com.beat.apis.config.GatewayConfig
import com.beat.apis.config.InfraConfig
import com.beat.gateway.EnableGatewayConfig
import com.beat.gateway.GatewayConfigGroup
import com.beat.gateway.GatewayModuleConfig
import com.beat.observability.ObservabilityModuleConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.nio.file.Path

class ApisApplicationTest {

    @Test
    fun `apis application keeps detached module import contract`() {
        val importAnnotation = ApisApplication::class.java.getAnnotation(Import::class.java)
        assertNotNull(importAnnotation, "ApisApplication must declare @Import")
        val importedClassNames = importAnnotation!!.value.map { it.java.name }.toSet()

        assertEquals(
            setOf(
                GatewayConfig::class.java.name,
                InfraConfig::class.java.name,
                ObservabilityModuleConfig::class.java.name,
            ),
            importedClassNames,
        )
    }

    @Test
    fun `apis application scans only the module owner namespace`() {
        val springBootApplication = ApisApplication::class.java.getAnnotation(SpringBootApplication::class.java)
        assertNotNull(springBootApplication)
        assertEquals(
            setOf(ApisApplication::class.java.name),
            springBootApplication!!.scanBasePackageClasses.map { it.java.name }.toSet(),
        )
    }

    @Test
    fun `apis selects gateway servlet security with refresh token store`() {
        val enableGatewayConfig = GatewayConfig::class.java.getAnnotation(EnableGatewayConfig::class.java)

        assertNotNull(enableGatewayConfig, "apis GatewayConfig must declare @EnableGatewayConfig")
        assertEquals(
            setOf(GatewayConfigGroup.SERVLET_SECURITY, GatewayConfigGroup.REFRESH_TOKEN_STORE),
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
    fun `apis security config exists for module owned route policy`() {
        val configuration = ApisSecurityConfig::class.java.getAnnotation(Configuration::class.java)
        assertNotNull(configuration)
    }

    @Test
    fun `apis swagger config keeps only general grouped docs ownership`() {
        val source = Files.readString(Path.of("src/main/java/com/beat/apis/swagger/config/SwaggerConfig.java"))

        assertTrue(source.contains(".group(\"general\")"))
        assertFalse(source.contains(".group(\"admin\")"))
        assertFalse(source.contains("pathsToMatch(\"/api/admin/**\")"))
    }

    @Test
    fun `apis application no longer owns broad component scan or transitional bootstrap import`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/beat/apis/ApisApplication.kt"))

        assertNull(ApisApplication::class.java.getAnnotation(ComponentScan::class.java))
        assertFalse(source.contains("ApisBootstrapConfig"))
        assertFalse(source.contains("\"com.beat.domain\""))
        assertFalse(source.contains("\"com.beat.global\""))
    }

    @Test
    fun `apis owner sources no longer declare legacy owner packages`() {
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
    fun `apis keeps schedule booking close job port bridge as module local no op contract`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/beat/apis/config/NoOpScheduleBookingCloseJobConfig.kt"))

        assertTrue(source.contains("@ConditionalOnMissingBean(ScheduleBookingCloseJobPort::class)"))
        assertTrue(source.contains("fun scheduleBookingCloseJobPort(): ScheduleBookingCloseJobPort = NoOpScheduleBookingCloseJobPort"))
        assertFalse(source.contains("JobSchedulerService"))
    }

    @Test
    fun `apis infra config keeps explicit base bootstrap groups`() {
        val configSource = Files.readString(Path.of("src/main/kotlin/com/beat/apis/config/InfraConfig.kt"))

        assertTrue(configSource.contains("InfraBaseConfigGroup.JPA"))
        assertFalse(configSource.contains("InfraBaseConfigGroup.QUERY_DSL"))
        assertFalse(configSource.contains("InfraBaseConfigGroup.REDIS"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.ASYNC"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.EXTERNAL_CLIENTS"))
    }

    @Test
    fun `apis application does not enable scheduling`() {
        val enableScheduling = ApisApplication::class.java.getAnnotation(EnableScheduling::class.java)
        assertNull(enableScheduling)
    }

    @Test
    fun `apis resources keep scheduler owner disabled`() {
        val config = Files.readString(Path.of("src/main/resources/application.yml"))

        assertTrue(config.contains("beat:"))
        assertTrue(config.contains("scheduler:"))
        assertTrue(config.contains("owner: false"))
        assertFalse(config.contains("owner: true"))
        assertTrue(config.contains("spring:"))
        assertTrue(config.contains("profiles:"))
        assertTrue(config.contains("group:"))
        assertTrue(config.contains("- persistence"))
        assertTrue(config.contains("- jwt"))
        assertTrue(config.contains("application-dev-secret.properties"))
        assertTrue(config.contains("application-prod-secret.properties"))
        assertTrue(config.contains("port: 4001"))
        assertFalse(config.contains("BEAT_SERVER_PORT"))
        assertFalse(config.contains("management:"))
        assertFalse(config.contains("../secret/application-dev-secret.properties"))
        assertFalse(config.contains("../secret/application-prod-secret.properties"))
    }

    @Test
    fun `apis test bootstrap does not rely on blanket bean overriding`() {
        val config = Files.readString(Path.of("src/test/resources/application-test.yml"))

        assertFalse(config.contains("allow-bean-definition-overriding"))
    }

    @Test
    fun `controller logging aspect is owned by observability module`() {
        assertFalse(Files.exists(Path.of("../src/main/java/com/beat/global/common/aop/ControllerLoggingAspect.java")))
        assertFalse(Files.exists(Path.of("../observability/src/main/java/com/beat/global/common/aop/ControllerLoggingAspect.java")))
        assertTrue(Files.exists(Path.of("../observability/src/main/java/com/beat/observability/aop/ControllerLoggingAspect.java")))
    }
}
