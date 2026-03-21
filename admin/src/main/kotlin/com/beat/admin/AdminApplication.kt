package com.beat.admin

import com.beat.admin.config.InfraConfig
import com.beat.gateway.GatewayModuleConfig
import com.beat.observability.ObservabilityModuleConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@SpringBootApplication(scanBasePackageClasses = [AdminApplication::class])
@EnableFeignClients(basePackages = ["com.beat.global"])
@ConfigurationPropertiesScan(basePackages = ["com.beat.infra.config"])
@ComponentScan(
    basePackages = [
        "com.beat.admin",
        "com.beat.domain",
        "com.beat.global",
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.REGEX,
            pattern = [
                "com\\.beat\\.domain\\..*\\.api\\..*",
                "com\\.beat\\.global\\.external\\.s3\\.api\\..*",
            ],
        ),
    ],
)
@Import(
    GatewayModuleConfig::class,
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
class AdminApplication

fun main(args: Array<String>) {
    runApplication<AdminApplication>(*args)
}
