package com.beat.apps.admin

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.mock.env.MockEnvironment

/**
 * Verifies that DB_HIKARI_MAX_POOL_SIZE env var (the actual override mechanism used by Ansible
 * per-module `env` maps) wins over the shared `${DB_HIKARI_MAX_POOL_SIZE}` placeholder declared in
 * infra's application-persistence.yml.
 *
 * Background: YAML documents belonging to a `spring.profiles.group` are loaded with HIGHER
 * precedence than the module's own application.yml (profile-specific or not) that declares the
 * group. This was confirmed empirically: an override placed directly in admin's application.yml did
 * NOT win over application-persistence.yml's default. Only an environment variable
 * (systemEnvironment property source, which always outranks config-data documents) reliably
 * supplies it.
 */
class HikariPoolSizeResolutionTest : FunSpec() {

    init {
        isolationMode = IsolationMode.SingleInstance

        test("DB_HIKARI_MAX_POOL_SIZE 환경 변수는 공통 persistence 기본값을 덮어쓴다") {
            val resolved = arrayOfNulls<String>(1)

            val app =
                SpringApplicationBuilder(AdminApplication::class.java)
                    .web(WebApplicationType.NONE)
                    .profiles("dev")
                    .initializers(
                        ApplicationContextInitializer<ConfigurableApplicationContext> { ctx ->
                            val mockEnv = MockEnvironment()
                            mockEnv.setProperty("DB_HIKARI_MAX_POOL_SIZE", "3")
                            ctx.environment.propertySources.addFirst(
                                mockEnv.propertySources.get("mockProperties")!!
                            )
                            resolved[0] =
                                ctx.environment.getProperty(
                                    "spring.datasource.hikari.maximum-pool-size"
                                )
                            throw AbortAfterEnvironmentResolved()
                        }
                    )

            try {
                app.run()
            } catch (_: AbortAfterEnvironmentResolved) {
                // expected: we only wanted the Environment, not a full context.
            }

            resolved[0] shouldBe "3"
        }
    }

    private class AbortAfterEnvironmentResolved : RuntimeException()
}
