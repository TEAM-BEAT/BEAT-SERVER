package com.beat.apis

import com.beat.apis.config.InfraConfig
import com.beat.gateway.GatewayModuleConfig
import com.beat.observability.ObservabilityModuleConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@SpringBootApplication(scanBasePackageClasses = [ApisApplication::class])
@EnableFeignClients(basePackages = ["com.beat.global"])
@ConfigurationPropertiesScan(basePackages = ["com.beat.infra.config"])
@ComponentScan(
    basePackages = [
        "com.beat.apis",
        "com.beat.domain",
        "com.beat.global",
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.REGEX,
            pattern = [
                "com\\.beat\\.admin\\..*",
                // auth/security beans → gateway bootstrap이 소유. broad scan 중복 방지.
                "com\\.beat\\.global\\.auth\\.jwt\\.filter\\..*",
                "com\\.beat\\.global\\.auth\\.jwt\\.provider\\..*",
                "com\\.beat\\.global\\.auth\\.resolver\\..*",
                "com\\.beat\\.global\\.auth\\.security\\..*",
                "com\\.beat\\.global\\.common\\.config\\.SecurityConfig",
                "com\\.beat\\.global\\.common\\.config\\.WebConfig",
            ],
        ),
    ],
)
@Import(
    GatewayModuleConfig::class,
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
class ApisApplication

fun main(args: Array<String>) {
    runApplication<ApisApplication>(*args)
}
