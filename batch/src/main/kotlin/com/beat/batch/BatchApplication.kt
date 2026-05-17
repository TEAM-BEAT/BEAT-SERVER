package com.beat.batch

import com.beat.batch.config.InfraConfig
import com.beat.observability.ObservabilityModuleConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackageClasses = [BatchApplication::class])
@EnableScheduling
@Import(
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}
