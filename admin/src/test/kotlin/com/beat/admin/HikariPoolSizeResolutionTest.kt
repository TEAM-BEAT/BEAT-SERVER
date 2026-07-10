package com.beat.admin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.mock.env.MockEnvironment

/**
 * Verifies that DB_HIKARI_MAX_POOL_SIZE env var (the actual override
 * mechanism used by Ansible per-module `env` maps) wins over the shared
 * `${DB_HIKARI_MAX_POOL_SIZE}` placeholder declared in infra's
 * application-persistence.yml.
 *
 * Background: YAML documents belonging to a `spring.profiles.group` are
 * loaded with HIGHER precedence than the module's own application.yml
 * (profile-specific or not) that declares the group. This was confirmed
 * empirically: an override placed directly in admin's application.yml did
 * NOT win over application-persistence.yml's default. Only an environment
 * variable (systemEnvironment property source, which always outranks
 * config-data documents) reliably supplies it.
 */
class HikariPoolSizeResolutionTest {

    @Test
    fun `DB_HIKARI_MAX_POOL_SIZE env var overrides shared persistence default`() {
        val resolved = arrayOfNulls<String>(1)

        val app = SpringApplicationBuilder(AdminApplication::class.java)
            .web(WebApplicationType.NONE)
            .profiles("dev")
            .initializers(ApplicationContextInitializer<ConfigurableApplicationContext> { ctx ->
                val mockEnv = MockEnvironment()
                mockEnv.setProperty("DB_HIKARI_MAX_POOL_SIZE", "3")
                ctx.environment.propertySources.addFirst(mockEnv.propertySources.get("mockProperties")!!)
                resolved[0] = ctx.environment.getProperty("spring.datasource.hikari.maximum-pool-size")
                throw AbortAfterEnvironmentResolved()
            })

        try {
            app.run()
        } catch (_: AbortAfterEnvironmentResolved) {
            // expected: we only wanted the Environment, not a full context.
        }

        assertEquals("3", resolved[0], "DB_HIKARI_MAX_POOL_SIZE env var must supply Hikari maximum-pool-size")
    }

    private class AbortAfterEnvironmentResolved : RuntimeException()
}
