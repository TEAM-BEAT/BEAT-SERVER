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

        test("admin application keeps detached module import contract") {
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

        test("admin selects gateway servlet security bootstrap without refresh token store") {
            val enableGatewayServletSecurity =
                GatewayConfig::class.java.getAnnotation(EnableGatewayServletSecurity::class.java)
            val enableGatewayConfig = GatewayConfig::class.java.getAnnotation(EnableGatewayConfig::class.java)

            enableGatewayServletSecurity shouldNotBe null
            enableGatewayConfig shouldBe null
            GatewayConfig::class.java.getAnnotation(Configuration::class.java) shouldNotBe null
        }

        test("admin security config exists for module owned route policy") {
            val configuration = AdminSecurityConfig::class.java.getAnnotation(Configuration::class.java)

            configuration shouldNotBe null
        }

        test("admin swagger config exists as non prod module owned documentation policy") {
            val profile = AdminSwaggerConfig::class.java.getAnnotation(Profile::class.java)
            profile shouldNotBe null
            profile!!.value.toSet() shouldBe setOf("!prod")

            val groupedOpenApi = AdminSwaggerConfig("").adminApi()
            groupedOpenApi.group shouldBe "admin"
            groupedOpenApi.pathsToMatch shouldBe listOf("/api/admin/**")
        }

        test("admin application no longer owns broad component scan") {
            val componentScan = AdminApplication::class.java.getAnnotation(ComponentScan::class.java)
            componentScan shouldBe null

            val springBootApplication = AdminApplication::class.java.getAnnotation(SpringBootApplication::class.java)
            springBootApplication shouldNotBe null
            springBootApplication!!.scanBasePackageClasses.map { it.java.name }.toSet() shouldBe
                setOf(AdminApplication::class.java.name)
        }

        test("admin application does not enable scheduling") {
            val enableScheduling = AdminApplication::class.java.getAnnotation(EnableScheduling::class.java)
            enableScheduling shouldBe null
        }

        test("admin infra config keeps explicit base bootstrap groups and imports") {
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

        test("admin resources keep scheduler owner disabled") {
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
