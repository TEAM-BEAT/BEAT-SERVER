package com.beat.apps.batch

import com.beat.apps.batch.config.InfraConfig
import com.beat.application.system.SystemApplicationConfig
import com.beat.observability.ObservabilityModuleConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackageClasses = [BatchApplication::class])
@EnableScheduling
@Import(
    SystemApplicationConfig::class,
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}
