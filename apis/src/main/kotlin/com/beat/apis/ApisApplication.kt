package com.beat.apis

import com.beat.apis.config.InfraConfig
import com.beat.gateway.EnableGatewayConfig
import com.beat.gateway.GatewayConfigGroup
import com.beat.observability.ObservabilityModuleConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(scanBasePackageClasses = [ApisApplication::class])
@ConfigurationPropertiesScan(basePackages = ["com.beat.infra.config"])
@EnableGatewayConfig(
    value = [
        GatewayConfigGroup.SERVLET_SECURITY,
        GatewayConfigGroup.REFRESH_TOKEN_STORE,
    ],
)
@Import(
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
class ApisApplication

fun main(args: Array<String>) {
    runApplication<ApisApplication>(*args)
}
