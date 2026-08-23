package com.beat.admin

import com.beat.admin.config.AdminSecurityConfig
import com.beat.admin.config.GatewayConfig
import com.beat.admin.config.InfraConfig
import com.beat.admin.swagger.config.AdminSwaggerConfig
import com.beat.application.admin.AdminApplicationConfig
import com.beat.infra.EnableInfraBaseConfig
import com.beat.infra.InfraBaseConfigGroup
import com.beat.infra.persistence.InfraPersistenceConfig
import com.beat.support.security.EnableGatewayConfig
import com.beat.support.security.EnableGatewayServletSecurity
import com.beat.observability.ObservabilityModuleConfig
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.nio.file.Path

class AdminApplicationTest : FunSpec() {

    init {
        isolationMode = IsolationMode.SingleInstance

        test("admin application은 분리된 모듈 import 계약을 유지한다") {
            val importAnnotation = AdminApplication::class.java.getAnnotation(Import::class.java)
            importAnnotation shouldNotBe null
            val importedClassNames = importAnnotation!!.value.map { it.java.name }.toSet()

            importedClassNames shouldBe setOf(
                AdminApplicationConfig::class.java.name,
                GatewayConfig::class.java.name,
                InfraConfig::class.java.name,
                ObservabilityModuleConfig::class.java.name,
            )
        }

        test("admin은 refresh token store 없이 gateway servlet security 부트스트랩을 선택한다") {
            val enableGatewayServletSecurity =
                GatewayConfig::class.java.getAnnotation(EnableGatewayServletSecurity::class.java)
            val enableGatewayConfig = GatewayConfig::class.java.getAnnotation(EnableGatewayConfig::class.java)

            enableGatewayServletSecurity shouldNotBe null
            enableGatewayConfig shouldBe null
            GatewayConfig::class.java.getAnnotation(Configuration::class.java) shouldNotBe null
        }

        test("admin 소유 경로 정책을 위해 AdminSecurityConfig가 존재한다") {
            val configuration = AdminSecurityConfig::class.java.getAnnotation(Configuration::class.java)

            configuration shouldNotBe null
        }

        test("prod 외 환경용 admin 소유 문서화 정책으로 AdminSwaggerConfig가 존재한다") {
            val profile = AdminSwaggerConfig::class.java.getAnnotation(Profile::class.java)
            profile shouldNotBe null
            profile!!.value.toSet() shouldBe setOf("!prod")

            val groupedOpenApi = AdminSwaggerConfig("").adminApi()
            groupedOpenApi.group shouldBe "admin"
            groupedOpenApi.pathsToMatch shouldBe listOf("/api/admin/**")
        }

        test("admin application은 더 이상 광범위한 component scan을 소유하지 않는다") {
            val componentScan = AdminApplication::class.java.getAnnotation(ComponentScan::class.java)
            componentScan shouldBe null

            val springBootApplication = AdminApplication::class.java.getAnnotation(SpringBootApplication::class.java)
            springBootApplication shouldNotBe null
            springBootApplication!!.scanBasePackageClasses.map { it.java.name }.toSet() shouldBe
                setOf(AdminApplication::class.java.name)
        }

        test("admin application은 scheduling을 활성화하지 않는다") {
            val enableScheduling = AdminApplication::class.java.getAnnotation(EnableScheduling::class.java)
            enableScheduling shouldBe null
        }

        test("admin infra config는 명시적인 base bootstrap group과 import를 유지한다") {
            InfraConfig::class.java.getAnnotation(Configuration::class.java) shouldNotBe null
            val enableInfraBaseConfig = InfraConfig::class.java.getAnnotation(EnableInfraBaseConfig::class.java)
            enableInfraBaseConfig shouldNotBe null
            enableInfraBaseConfig!!.value.toSet() shouldBe setOf(
                InfraBaseConfigGroup.JPA,
                InfraBaseConfigGroup.EXTERNAL_CLIENTS,
            )

            val imports = InfraConfig::class.java.getAnnotation(Import::class.java)
            imports shouldNotBe null
            imports!!.value.map { it.java.name }.toSet() shouldBe setOf(InfraPersistenceConfig::class.java.name)
        }

        test("admin 리소스는 scheduler owner 비활성 설정을 유지한다") {
            val config = Files.readString(Path.of("src/main/resources/application.yml"))

            config.contains("beat:") shouldBe true
            config.contains("scheduler:") shouldBe true
            config.contains("owner: false") shouldBe true
            config.contains("owner: true") shouldBe false
            config.contains("profiles:") shouldBe true
            config.contains("group:") shouldBe true
            config.contains("- persistence") shouldBe true
            config.contains("- jwt") shouldBe true
            config.contains("application-dev-secret.properties") shouldBe true
            config.contains("application-prod-secret.properties") shouldBe true
            config.contains("port: 4000") shouldBe true
            config.contains("BEAT_SERVER_PORT") shouldBe false
            config.contains("management:") shouldBe false
            config.contains("../secret/application-dev-secret.properties") shouldBe false
            config.contains("../secret/application-prod-secret.properties") shouldBe false
        }
    }
}
