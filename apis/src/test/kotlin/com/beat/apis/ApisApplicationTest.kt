package com.beat.apis

import com.beat.application.frontoffice.FrontofficeApplicationConfig
import com.beat.apis.config.ApisSecurityConfig
import com.beat.apis.config.GatewayConfig
import com.beat.apis.config.GuestSessionOriginFilter
import com.beat.apis.config.InfraConfig
import com.beat.contracts.auth.guest.GuestAccessThrottlePort
import com.beat.contracts.auth.guest.GuestSessionPort
import com.beat.application.frontoffice.auth.command.RefreshTokenStore
import com.beat.support.security.EnableGatewayConfig
import com.beat.support.security.GatewayConfigGroup
import com.beat.support.security.EnableGatewayServletSecurity
import com.beat.infra.InfraBaseConfigGroup
import com.beat.infra.redis.auth.AuthRedisConfig
import com.beat.observability.ObservabilityModuleConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.nio.file.Path
import jakarta.servlet.http.Cookie

class ApisApplicationTest {

    @Test
    fun `apis application keeps detached module import contract`() {
        val importAnnotation = ApisApplication::class.java.getAnnotation(Import::class.java)
        assertNotNull(importAnnotation, "ApisApplication must declare @Import")
        val importedClassNames = importAnnotation!!.value.map { it.java.name }.toSet()

        assertEquals(
            setOf(
                FrontofficeApplicationConfig::class.java.name,
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
    fun `apis selects gateway servlet security bootstrap with guest password hash`() {
        val enableGatewayServletSecurity = GatewayConfig::class.java.getAnnotation(EnableGatewayServletSecurity::class.java)
        val enableGatewayConfig = GatewayConfig::class.java.getAnnotation(EnableGatewayConfig::class.java)

        assertNotNull(enableGatewayServletSecurity, "apis GatewayConfig must declare @EnableGatewayServletSecurity")
        assertNotNull(enableGatewayConfig, "apis GatewayConfig must declare @EnableGatewayConfig for guest password hash")
        assertEquals(
            setOf(
                GatewayConfigGroup.GUEST_ACCESS,
            ),
            enableGatewayConfig!!.value.toSet(),
        )
    }

    @Test
    fun `auth Redis config가 application refresh token store requirement를 충족한다`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration::class.java))
            .withUserConfiguration(
                AuthRedisConfig::class.java,
            )
            .run { context ->
                assertTrue(context.startupFailure == null, context.startupFailure?.message)
                assertEquals(1, context.getBeansOfType(RefreshTokenStore::class.java).size)
                assertEquals(1, context.getBeansOfType(GuestSessionPort::class.java).size)
                assertEquals(1, context.getBeansOfType(GuestAccessThrottlePort::class.java).size)
                val scripts = context.getBeansOfType(RedisScript::class.java)
                assertEquals(1, scripts.size)
                assertTrue(scripts.values.single().scriptAsString.contains("redis.call('INCR'"))
            }
    }

    @Test
    fun `apis security config exists for module owned route policy`() {
        val configuration = ApisSecurityConfig::class.java.getAnnotation(Configuration::class.java)
        assertNotNull(configuration)
    }

    @Test
    fun `apis swagger config keeps only general grouped docs ownership`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/beat/apis/swagger/config/SwaggerConfig.kt"))

        assertTrue(source.contains("@Profile(\"!prod\")"))
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
    fun `apis infra config keeps explicit base bootstrap groups`() {
        val configSource = Files.readString(Path.of("src/main/kotlin/com/beat/apis/config/InfraConfig.kt"))

        assertTrue(configSource.contains("InfraBaseConfigGroup.JPA"))
        assertFalse(configSource.contains("InfraBaseConfigGroup.AUTH_REDIS"))
        assertTrue(configSource.contains("AuthRedisConfig::class"))
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
        assertTrue(config.contains("forward-headers-strategy: native"))
        assertFalse(config.contains("BEAT_SERVER_PORT"))
        assertFalse(config.contains("management:"))
        assertFalse(config.contains("../secret/application-dev-secret.properties"))
        assertFalse(config.contains("../secret/application-prod-secret.properties"))
    }

    @Test
    fun `guest cookie mutation requires an allowed origin`() {
        val filter = GuestSessionOriginFilter(
            arrayOf("https://client.example"),
            AccessDeniedHandler { _, response, _ -> response.sendError(HttpStatus.FORBIDDEN.value()) },
        )

        val missingOrigin = guestMutationRequest()
        val missingOriginResponse = MockHttpServletResponse()
        filter.doFilter(missingOrigin, missingOriginResponse, MockFilterChain())
        assertEquals(HttpStatus.FORBIDDEN.value(), missingOriginResponse.status)

        val disallowedOrigin = guestMutationRequest("https://attacker.example")
        val disallowedOriginResponse = MockHttpServletResponse()
        filter.doFilter(disallowedOrigin, disallowedOriginResponse, MockFilterChain())
        assertEquals(HttpStatus.FORBIDDEN.value(), disallowedOriginResponse.status)

        val allowedOrigin = guestMutationRequest("https://client.example")
        val allowedOriginResponse = MockHttpServletResponse()
        val allowedChain = MockFilterChain()
        filter.doFilter(allowedOrigin, allowedOriginResponse, allowedChain)
        assertEquals(HttpStatus.OK.value(), allowedOriginResponse.status)
        assertNotNull(allowedChain.request)
    }

    private fun guestMutationRequest(origin: String? = null): MockHttpServletRequest =
        MockHttpServletRequest("PATCH", "/api/bookings/refund").apply {
            setCookies(Cookie("__Host-guestSession", "token"))
            origin?.let { addHeader("Origin", it) }
        }

    @Test
    fun `apis test bootstrap does not rely on blanket bean overriding`() {
        val config = Files.readString(Path.of("src/test/resources/application-test.yml"))

        assertFalse(config.contains("allow-bean-definition-overriding"))
    }

    @Test
    fun `observability legacy logging package is removed`() {
        val legacyLoggingPackage = Path.of("../observability/src/main/java/com/beat/observability", "aop")

        assertFalse(Files.exists(legacyLoggingPackage))
    }

    @Test
    fun `apis security chain registers gateway mdc filter through public filter contract`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/beat/apis/config/ApisSecurityConfig.kt"))

        assertTrue(source.contains("@param:Qualifier(\"gatewayJwtAuthenticationFilter\")"))
        assertTrue(source.contains("@param:Qualifier(\"gatewaySecurityMdcLoggingFilter\")"))
        assertTrue(source.contains(".addFilterBefore(securityMdcLoggingFilter, UsernamePasswordAuthenticationFilter::class.java)"))
        assertTrue(source.contains(".addFilterAfter(jwtAuthenticationFilter, securityMdcLoggingFilter.javaClass)"))
        assertTrue(source.contains("val SWAGGER_WHITELIST"))
        assertTrue(source.contains("if (!environment.acceptsProfiles(Profiles.of(\"prod\")))"))
        assertTrue(source.contains("addAll(SWAGGER_WHITELIST)"))
        assertFalse(source.contains("import com.beat.support.security.authentication.internal.SecurityMdcLoggingFilter"))
    }

    @Test
    fun `apis prod resources disable springdoc endpoints`() {
        val config = Files.readString(Path.of("src/main/resources/application.yml"))
        val prodSection = config.substringAfter("on-profile: prod")

        assertTrue(
            prodSection.contains(
                """
                springdoc:
                  swagger-ui:
                    enabled: false
                  api-docs:
                    enabled: false
                """.trimIndent(),
            ),
        )
    }
}
