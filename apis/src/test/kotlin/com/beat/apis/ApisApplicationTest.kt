package com.beat.apis

import com.beat.apis.config.InfraConfig
import com.beat.gateway.GatewayModuleConfig
import com.beat.gateway.bootstrap.GatewayAuthBootstrapConfig
import com.beat.gateway.bootstrap.GatewayAuthImportSelector
import com.beat.observability.ObservabilityModuleConfig
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.core.type.AnnotationMetadata
import org.springframework.scheduling.annotation.EnableScheduling

class ApisApplicationTest {

    @Test
    fun `apis application keeps transition baseline module imports`() {
        val importAnnotation = ApisApplication::class.java.getAnnotation(Import::class.java)
        val importedClassNames = importAnnotation.value.map { it.java.name }.toSet()

        assertTrue(importedClassNames.contains(GatewayModuleConfig::class.java.name))
        assertTrue(importedClassNames.contains(InfraConfig::class.java.name))
        assertTrue(importedClassNames.contains(ObservabilityModuleConfig::class.java.name))
    }

    @Test
    fun `gateway module imports auth bootstrap config`() {
        val importAnnotation = GatewayModuleConfig::class.java.getAnnotation(Import::class.java)
        assertNotNull(importAnnotation)
        val imported = importAnnotation.value.map { it.java.name }.toSet()
        assertTrue(imported.contains(GatewayAuthBootstrapConfig::class.java.name))
    }

    @Test
    fun `gateway auth bootstrap imports all auth and security beans`() {
        val selector = GatewayAuthImportSelector()
        val imported = selector.selectImports(
            AnnotationMetadata.introspect(GatewayAuthBootstrapConfig::class.java),
        ).toSet()

        // auth beans
        assertTrue(imported.contains("com.beat.global.auth.jwt.filter.JwtAuthenticationFilter"))
        assertTrue(imported.contains("com.beat.global.auth.jwt.provider.JwtTokenProvider"))
        assertTrue(imported.contains("com.beat.global.auth.resolver.CurrentMemberArgumentResolver"))
        assertTrue(imported.contains("com.beat.global.auth.security.CustomAccessDeniedHandler"))
        assertTrue(imported.contains("com.beat.global.auth.security.CustomJwtAuthenticationEntryPoint"))

        // config beans
        assertTrue(imported.contains("com.beat.global.common.config.SecurityConfig"))
        assertTrue(imported.contains("com.beat.global.common.config.WebConfig"))
    }

    @Test
    fun `apis broad scan excludes auth packages owned by gateway`() {
        val scan = ApisApplication::class.java.getAnnotation(ComponentScan::class.java)
        assertNotNull(scan)

        val excluded = scan.excludeFilters.flatMap { it.pattern.toList() }.toSet()

        setOf(
            "com\\.beat\\.global\\.auth\\.jwt\\.filter\\..*",
            "com\\.beat\\.global\\.auth\\.jwt\\.provider\\..*",
            "com\\.beat\\.global\\.auth\\.resolver\\..*",
            "com\\.beat\\.global\\.auth\\.security\\..*",
            "com\\.beat\\.global\\.common\\.config\\.SecurityConfig",
            "com\\.beat\\.global\\.common\\.config\\.WebConfig",
        ).forEach { pattern ->
            assertTrue(excluded.contains(pattern), "missing exclude: $pattern")
        }
    }

    @Test
    fun `apis infra config keeps explicit base bootstrap groups`() {
        val configSource = Files.readString(Path.of("src/main/kotlin/com/beat/apis/config/InfraConfig.kt"))

        assertTrue(configSource.contains("InfraBaseConfigGroup.JPA"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.QUERY_DSL"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.REDIS"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.ASYNC"))
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
    }
}
