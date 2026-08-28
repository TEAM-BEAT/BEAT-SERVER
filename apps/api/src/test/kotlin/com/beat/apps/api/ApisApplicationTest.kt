package com.beat.apps.api

import com.beat.application.frontoffice.FrontofficeApplicationConfig
import com.beat.application.frontoffice.auth.command.RefreshTokenStore
import com.beat.application.frontoffice.booking.booker.command.GuestAccessThrottle
import com.beat.application.frontoffice.booking.booker.command.GuestSessionStore
import com.beat.apps.api.booking.web.GuestSessionOriginFilter
import com.beat.apps.api.config.ApisSecurityConfig
import com.beat.apps.api.config.GatewayConfig
import com.beat.apps.api.config.InfraConfig
import com.beat.apps.api.swagger.config.SwaggerConfig
import com.beat.infrastructure.EnableInfraBaseConfig
import com.beat.infrastructure.InfraBaseConfigGroup
import com.beat.infrastructure.persistence.InfraPersistenceConfig
import com.beat.infrastructure.redis.auth.AuthRedisConfig
import com.beat.support.observability.ObservabilityModuleConfig
import com.beat.support.security.EnableGatewayConfig
import com.beat.support.security.EnableGatewayServletSecurity
import com.beat.support.security.GatewayConfigGroup
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.servlet.http.Cookie
import java.nio.file.Files
import java.nio.file.Path
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.web.access.AccessDeniedHandler

class ApisApplicationTest : FunSpec() {

    init {
        isolationMode = IsolationMode.SingleInstance

        test("apis application은 공개 composition 타입만 import하고 narrow bootstrap을 포함한다") {
            val importAnnotation = ApisApplication::class.java.getAnnotation(Import::class.java)
            importAnnotation shouldNotBe null
            val importedClassNames = importAnnotation!!.value.map { it.java.name }.toSet()

            val publicCompositionTypes =
                setOf(
                    FrontofficeApplicationConfig::class.java.name,
                    GatewayConfig::class.java.name,
                    InfraConfig::class.java.name,
                    ObservabilityModuleConfig::class.java.name,
                )
            // 신규 공개 구성 추가는 이 허용목록 확장을 강제하고, 내부 구현 import는 즉시 실패한다.
            (importedClassNames - publicCompositionTypes) shouldBe emptySet()
            importedClassNames.contains(FrontofficeApplicationConfig::class.java.name) shouldBe true
        }

        test("apis application은 모듈 소유 네임스페이스만 스캔한다") {
            val springBootApplication =
                ApisApplication::class.java.getAnnotation(SpringBootApplication::class.java)
            springBootApplication shouldNotBe null
            springBootApplication!!.scanBasePackageClasses.map { it.java.name }.toSet() shouldBe
                setOf(ApisApplication::class.java.name)
        }

        test("apis는 guest password hash와 함께 gateway servlet security 부트스트랩을 선택한다") {
            val enableGatewayServletSecurity =
                GatewayConfig::class.java.getAnnotation(EnableGatewayServletSecurity::class.java)
            val enableGatewayConfig =
                GatewayConfig::class.java.getAnnotation(EnableGatewayConfig::class.java)

            enableGatewayServletSecurity shouldNotBe null
            enableGatewayConfig shouldNotBe null
            GatewayConfig::class.java.getAnnotation(Configuration::class.java) shouldNotBe null
            enableGatewayConfig!!.value.toSet() shouldBe setOf(GatewayConfigGroup.GUEST_ACCESS)
        }

        test("auth Redis config가 application refresh token store requirement를 충족한다") {
            ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration::class.java))
                .withUserConfiguration(AuthRedisConfig::class.java)
                .run { context ->
                    (context.startupFailure == null) shouldBe true
                    context.getBeansOfType(RefreshTokenStore::class.java).size shouldBe 1
                    context.getBeansOfType(GuestSessionStore::class.java).size shouldBe 1
                    context.getBeansOfType(GuestAccessThrottle::class.java).size shouldBe 1
                    val scripts = context.getBeansOfType(RedisScript::class.java)
                    scripts.size shouldBe 1
                    scripts.values.single().scriptAsString.contains("redis.call('INCR'") shouldBe
                        true
                }
        }

        test("apis 소유 경로 정책을 위해 ApisSecurityConfig가 존재한다") {
            ApisSecurityConfig::class.java.getAnnotation(Configuration::class.java) shouldNotBe null
        }

        test("apis swagger config는 general 그룹 문서 소유권만 유지한다") {
            val profile = SwaggerConfig::class.java.getAnnotation(Profile::class.java)
            profile shouldNotBe null
            profile!!.value.toSet() shouldBe setOf("!prod")

            val groupedOpenApi = SwaggerConfig("https://api.example").generalApi()
            groupedOpenApi.group shouldBe "general"
            groupedOpenApi.pathsToMatch shouldBe listOf("/**")
        }

        test("apis application은 모듈 소유 component scan만 유지한다") {
            ApisApplication::class.java.getAnnotation(ComponentScan::class.java) shouldBe null
        }

        test("apis infra config는 명시적인 base bootstrap group과 import를 유지한다") {
            InfraConfig::class.java.getAnnotation(Configuration::class.java) shouldNotBe null
            val enableInfraBaseConfig =
                InfraConfig::class.java.getAnnotation(EnableInfraBaseConfig::class.java)
            enableInfraBaseConfig shouldNotBe null
            enableInfraBaseConfig!!.value.toSet() shouldBe
                setOf(
                    InfraBaseConfigGroup.JPA,
                    InfraBaseConfigGroup.ASYNC,
                    InfraBaseConfigGroup.EXTERNAL_CLIENTS,
                )

            val imports = InfraConfig::class.java.getAnnotation(Import::class.java)
            imports shouldNotBe null
            imports!!.value.map { it.java.name }.toSet() shouldBe
                setOf(
                    InfraPersistenceConfig::class.java.name,
                    AuthRedisConfig::class.java.name,
                )
        }

        test("apis application은 scheduling을 활성화하지 않는다") {
            ApisApplication::class.java.getAnnotation(EnableScheduling::class.java) shouldBe null
        }

        test("apis 리소스는 scheduler owner 비활성 설정을 유지한다") {
            val config = Files.readString(Path.of("src/main/resources/application.yml"))

            config.contains("beat:") shouldBe true
            config.contains("scheduler:") shouldBe true
            config.contains("owner: false") shouldBe true
            config.contains("owner: true") shouldBe false
            config.contains("spring:") shouldBe true
            config.contains("profiles:") shouldBe true
            config.contains("group:") shouldBe true
            config.contains("- persistence") shouldBe true
            config.contains("- jwt") shouldBe true
            config.contains("application-dev-secret.properties") shouldBe true
            config.contains("application-prod-secret.properties") shouldBe true
            config.contains("port: 4001") shouldBe true
            config.contains("forward-headers-strategy: native") shouldBe true
            config.contains("BEAT_SERVER_PORT") shouldBe false
            config.contains("management:") shouldBe false
            config.contains("../secret/application-dev-secret.properties") shouldBe false
            config.contains("../secret/application-prod-secret.properties") shouldBe false

            val prodSection = config.substringAfter("on-profile: prod")
            prodSection.contains(
                """
                springdoc:
                  swagger-ui:
                    enabled: false
                  api-docs:
                    enabled: false
                """
                    .trimIndent()
            ) shouldBe true
        }

        test("guest cookie 변경은 허용된 origin에서만 가능하다") {
            val filter =
                GuestSessionOriginFilter(
                    arrayOf("https://client.example"),
                    AccessDeniedHandler { _, response, _ ->
                        response.sendError(HttpStatus.FORBIDDEN.value())
                    },
                )

            val missingOrigin = guestMutationRequest()
            val missingOriginResponse = MockHttpServletResponse()
            filter.doFilter(missingOrigin, missingOriginResponse, MockFilterChain())
            missingOriginResponse.status shouldBe HttpStatus.FORBIDDEN.value()

            val disallowedOrigin = guestMutationRequest("https://attacker.example")
            val disallowedOriginResponse = MockHttpServletResponse()
            filter.doFilter(disallowedOrigin, disallowedOriginResponse, MockFilterChain())
            disallowedOriginResponse.status shouldBe HttpStatus.FORBIDDEN.value()

            val allowedOrigin = guestMutationRequest("https://client.example")
            val allowedOriginResponse = MockHttpServletResponse()
            val allowedChain = MockFilterChain()
            filter.doFilter(allowedOrigin, allowedOriginResponse, allowedChain)
            allowedOriginResponse.status shouldBe HttpStatus.OK.value()
            allowedChain.request shouldNotBe null
        }
    }

    private fun guestMutationRequest(origin: String? = null): MockHttpServletRequest =
        MockHttpServletRequest("PATCH", "/api/bookings/refund").apply {
            setCookies(Cookie("__Host-guestSession", "token"))
            origin?.let { addHeader("Origin", it) }
        }
}
