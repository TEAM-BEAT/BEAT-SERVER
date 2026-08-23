package com.beat.batch

import com.beat.batch.config.InfraConfig
import com.beat.application.system.SystemApplicationConfig
import com.beat.infra.EnableInfraBaseConfig
import com.beat.infra.InfraBaseConfigGroup
import com.beat.infra.persistence.InfraPersistenceConfig
import com.beat.observability.ObservabilityModuleConfig
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.nio.file.Path

class BatchApplicationTest : FunSpec() {

    init {
        isolationMode = IsolationMode.SingleInstance

        test("batch application은 분리된 모듈 import 계약을 유지한다") {
            val importAnnotation = BatchApplication::class.java.getAnnotation(Import::class.java)
            importAnnotation shouldNotBe null
            val importedClassNames = importAnnotation!!.value.map { it.java.name }.toSet()

            importedClassNames shouldBe setOf(
                SystemApplicationConfig::class.java.name,
                InfraConfig::class.java.name,
                ObservabilityModuleConfig::class.java.name,
            )
        }

        test("batch application은 scheduling을 모듈 부트스트랩 안에 유지한다") {
            val springBootApplication = BatchApplication::class.java.getAnnotation(SpringBootApplication::class.java)
            val componentScan = BatchApplication::class.java.getAnnotation(ComponentScan::class.java)
            val enableScheduling = BatchApplication::class.java.getAnnotation(EnableScheduling::class.java)

            springBootApplication shouldNotBe null
            componentScan shouldBe null
            enableScheduling shouldNotBe null
            springBootApplication!!.scanBasePackageClasses.map { it.java.name }.toSet() shouldBe
                setOf(BatchApplication::class.java.name)
        }

        test("batch infra config는 명시적인 base bootstrap group을 유지한다") {
            InfraConfig::class.java.getAnnotation(Configuration::class.java) shouldNotBe null
            val enableInfraBaseConfig = InfraConfig::class.java.getAnnotation(EnableInfraBaseConfig::class.java)
            enableInfraBaseConfig shouldNotBe null
            enableInfraBaseConfig!!.value.toSet() shouldBe setOf(
                InfraBaseConfigGroup.JPA,
                InfraBaseConfigGroup.ASYNC,
            )

            val imports = InfraConfig::class.java.getAnnotation(Import::class.java)
            imports shouldNotBe null
            imports!!.value.map { it.java.name }.toSet() shouldBe setOf(InfraPersistenceConfig::class.java.name)
        }

        test("batch 리소스는 기본으로 scheduler 소유를 활성화한다") {
            val config = Files.readString(Path.of("src/main/resources/application.yml"))

            config.contains("beat:") shouldBe true
            config.contains("scheduler:") shouldBe true
            config.contains("owner: true") shouldBe true
            config.contains("owner: false") shouldBe false
            config.contains("profiles:") shouldBe true
            config.contains("group:") shouldBe true
            config.contains("- persistence") shouldBe true
            config.contains("- observability") shouldBe true
            config.contains("- thread-pool") shouldBe true
            config.contains("- jwt") shouldBe false
            config.contains("- redis") shouldBe false
            config.contains("- external") shouldBe false
            config.contains("on-profile: dev") shouldBe true
            config.contains("application-dev-secret.properties") shouldBe true
            config.contains("port: 4002") shouldBe true
            config.contains("on-profile: prod") shouldBe true
            config.contains("application-prod-secret.properties") shouldBe true
            config.contains("BEAT_SERVER_PORT") shouldBe false
            config.contains("management:") shouldBe false
            config.contains("../secret/application-dev-secret.properties") shouldBe false
            config.contains("../secret/application-prod-secret.properties") shouldBe false
            config.contains("datasource:") shouldBe false
        }
    }
}
