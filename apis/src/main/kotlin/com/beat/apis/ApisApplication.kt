package com.beat.apis

import com.beat.apis.config.GatewayConfig
import com.beat.apis.config.InfraConfig
import com.beat.observability.ObservabilityModuleConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(scanBasePackageClasses = [ApisApplication::class])
@Import(
    GatewayConfig::class,
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
class ApisApplication

fun main(args: Array<String>) {
    runApplication<ApisApplication>(*args)
}
