package com.beat.admin

import com.beat.admin.config.GatewayConfig
import com.beat.admin.config.InfraConfig
import com.beat.observability.ObservabilityModuleConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(scanBasePackageClasses = [AdminApplication::class])
@ConfigurationPropertiesScan(basePackages = ["com.beat.infra.config"])
@Import(
    GatewayConfig::class,
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
class AdminApplication

fun main(args: Array<String>) {
    runApplication<AdminApplication>(*args)
}
