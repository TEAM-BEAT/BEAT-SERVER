package com.beat.batch

import com.beat.batch.config.BatchSchedulerBootstrapConfig
import com.beat.batch.config.InfraConfig
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
import org.springframework.scheduling.annotation.EnableScheduling

class BatchApplicationTest {

    @Test
    fun `batch application keeps transition baseline scheduling contract`() {
        val importAnnotation = BatchApplication::class.java.getAnnotation(Import::class.java)
        val importedClassNames = importAnnotation.value.map { it.java.name }.toSet()
        val applicationComponentScan = BatchApplication::class.java.getAnnotation(ComponentScan::class.java)
        val bootstrapImport = BatchSchedulerBootstrapConfig::class.java.getAnnotation(Import::class.java)
        val enableScheduling = BatchApplication::class.java.getAnnotation(EnableScheduling::class.java)

        assertTrue(importedClassNames.contains(BatchSchedulerBootstrapConfig::class.java.name))
        assertTrue(importedClassNames.contains(InfraConfig::class.java.name))
        assertTrue(importedClassNames.contains(ObservabilityModuleConfig::class.java.name))
        assertFalse(importedClassNames.contains("com.beat.gateway.GatewayModuleConfig"))
        assertNull(applicationComponentScan)
        assertNotNull(bootstrapImport)
        val bootstrapClassNames = bootstrapImport.value.map { it.java.name }.toSet()
        assertTrue(bootstrapClassNames.contains("com.beat.global.common.scheduler.application.JobSchedulerService"))
        assertTrue(bootstrapClassNames.contains("com.beat.global.common.scheduler.application.JobSchedulerTransactionalService"))
        assertTrue(bootstrapClassNames.contains("com.beat.domain.booking.application.TicketCleanupScheduler"))
        assertTrue(bootstrapClassNames.contains("com.beat.domain.promotion.application.PromotionSchedulerService"))
        assertTrue(enableScheduling != null)
    }

    @Test
    fun `batch resources enable scheduler ownership by default`() {
        val config = Files.readString(Path.of("src/main/resources/application.yml"))
        assertTrue(config.contains("beat:"))
        assertTrue(config.contains("scheduler:"))
        assertTrue(config.contains("owner: true"))
        assertFalse(config.contains("owner: false"))
    }
}
