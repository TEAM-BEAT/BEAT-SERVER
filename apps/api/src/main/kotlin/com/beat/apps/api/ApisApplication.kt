package com.beat.apps.api

import com.beat.application.frontoffice.FrontofficeApplicationConfig
import com.beat.apps.api.config.GatewayConfig
import com.beat.apps.api.config.InfraConfig
import com.beat.observability.ObservabilityModuleConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(scanBasePackageClasses = [ApisApplication::class])
@Import(
    FrontofficeApplicationConfig::class,
    GatewayConfig::class,
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
class ApisApplication

fun main(args: Array<String>) {
    runApplication<ApisApplication>(*args)
}
