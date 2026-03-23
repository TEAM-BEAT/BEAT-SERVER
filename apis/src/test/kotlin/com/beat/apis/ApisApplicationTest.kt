package com.beat.apis

import com.beat.apis.config.ApisBootstrapConfig
import com.beat.apis.config.ApisScheduleJobPortConfig
import com.beat.apis.config.ApisSecurityConfig
import com.beat.apis.config.InfraConfig
import com.beat.gateway.GatewayModuleConfig
import com.beat.observability.ObservabilityModuleConfig
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

class ApisApplicationTest {

    @Test
    fun `apis application keeps transition baseline module imports`() {
        val importAnnotation = ApisApplication::class.java.getAnnotation(Import::class.java)
        val importedClassNames = importAnnotation.value.map { it.java.name }.toSet()

        assertTrue(importedClassNames.contains(ApisBootstrapConfig::class.java.name))
        assertTrue(importedClassNames.contains(GatewayModuleConfig::class.java.name))
        assertTrue(importedClassNames.contains(InfraConfig::class.java.name))
        assertTrue(importedClassNames.contains(ObservabilityModuleConfig::class.java.name))
    }

    @Test
    fun `gateway module imports auth bootstrap config`() {
        val componentScan = GatewayModuleConfig::class.java.getAnnotation(ComponentScan::class.java)
        assertNotNull(componentScan)
        assertTrue(componentScan.basePackages.contains("com.beat.gateway"))
    }

    @Test
    fun `apis security config exists for module-owned route policy`() {
        val configuration = ApisSecurityConfig::class.java.getAnnotation(Configuration::class.java)
        assertNotNull(configuration)
    }

    @Test
    fun `apis application no longer owns broad component scan`() {
        val scan = ApisApplication::class.java.getAnnotation(ComponentScan::class.java)
        assertNull(scan)
    }

    @Test
    fun `apis bootstrap config scans targeted application packages without root scheduler bridge`() {
        val scan = ApisBootstrapConfig::class.java.getAnnotation(ComponentScan::class.java)
        assertNotNull(scan)

        val scannedClassNames = scan.basePackageClasses.map { it.java.name }.toSet()
        assertTrue(scannedClassNames.contains("com.beat.domain.booking.api.BookingController"))
        assertTrue(scannedClassNames.contains("com.beat.domain.booking.application.TicketService"))
        assertTrue(scannedClassNames.contains("com.beat.domain.member.application.MemberService"))
        assertTrue(scannedClassNames.contains("com.beat.domain.performance.application.PerformanceService"))
        assertTrue(scannedClassNames.contains("com.beat.domain.schedule.application.ScheduleService"))
        assertTrue(scannedClassNames.contains("com.beat.domain.promotion.application.PromotionService"))
        assertTrue(scannedClassNames.contains("com.beat.domain.user.application.UserService"))
        assertTrue(scannedClassNames.contains("com.beat.global.external.s3.api.FileController"))
        assertTrue(scannedClassNames.contains("com.beat.global.external.notification.slack.event.BookingCreatedEventListener"))
        assertFalse(scannedClassNames.contains("com.beat.global.common.scheduler.application.JobSchedulerService"))
        assertTrue(scan.excludeFilters.isEmpty())
    }

    @Test
    fun `apis keeps schedule job port bridge inside apis config`() {
        val configuration = ApisScheduleJobPortConfig::class.java.getAnnotation(Configuration::class.java)
        assertNotNull(configuration)
    }

    @Test
    fun `apis infra config keeps explicit base bootstrap groups`() {
        val configSource = Files.readString(Path.of("src/main/kotlin/com/beat/apis/config/InfraConfig.kt"))

        assertTrue(configSource.contains("InfraBaseConfigGroup.JPA"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.QUERY_DSL"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.REDIS"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.ASYNC"))
        assertTrue(configSource.contains("InfraBaseConfigGroup.EXTERNAL_CLIENTS"))
    }

    @Test
    fun `apis application does not enable scheduling`() {
        val enableScheduling = ApisApplication::class.java.getAnnotation(EnableScheduling::class.java)
        assertNull(enableScheduling)
    }

    @Test
    fun `apis application no longer owns feign bootstrap scanning`() {
        val source = Files.readString(Path.of("src/main/kotlin/com/beat/apis/ApisApplication.kt"))
        assertFalse(source.contains("@EnableFeignClients"))
        assertFalse(source.contains("\"com.beat.domain\""))
        assertFalse(source.contains("\"com.beat.global\""))
    }

    @Test
    fun `apis member lane no longer imports root social auth client packages`() {
        listOf(
            "src/main/java/com/beat/domain/member/application/SocialLoginService.java",
            "src/main/java/com/beat/domain/member/application/MemberRegistrationService.java",
            "src/main/java/com/beat/domain/member/application/AuthenticationService.java",
            "src/main/java/com/beat/domain/member/api/MemberController.java",
            "src/main/java/com/beat/domain/member/api/MemberApi.java",
        ).forEach { relativePath ->
            val source = Files.readString(Path.of(relativePath))
            assertFalse(source.contains("com.beat.global.auth.client"))
        }
    }

    @Test
    fun `apis resources keep scheduler owner disabled`() {
        val config = Files.readString(Path.of("src/main/resources/application.yml"))

        assertTrue(config.contains("beat:"))
        assertTrue(config.contains("scheduler:"))
        assertTrue(config.contains("owner: false"))
        assertFalse(config.contains("owner: true"))
    }

    @Test
    fun `controller logging aspect is owned by observability module`() {
        assertFalse(Files.exists(Path.of("../src/main/java/com/beat/global/common/aop/ControllerLoggingAspect.java")))
        assertTrue(Files.exists(Path.of("../observability/src/main/java/com/beat/global/common/aop/ControllerLoggingAspect.java")))
    }
}
