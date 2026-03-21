package com.beat.batch

import com.beat.batch.config.InfraConfig
import com.beat.observability.ObservabilityModuleConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackageClasses = [BatchApplication::class])
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = ["com.beat.infra.config"])
@ComponentScan(
    basePackages = [
        "com.beat.batch",
        "com.beat.domain",
        "com.beat.global.common.scheduler",
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.REGEX,
            pattern = [
                "com\\.beat\\.domain\\..*\\.api\\..*",
                "com\\.beat\\.admin\\..*",
                "com\\.beat\\.global\\.external\\.s3\\.api\\..*",
                "com\\.beat\\.global\\.common\\.config\\.SecurityConfig",
                "com\\.beat\\.global\\.common\\.config\\.WebConfig",
                "com\\.beat\\.global\\.swagger\\..*",
            ],
        ),
    ],
)
@Import(
    InfraConfig::class,
    ObservabilityModuleConfig::class,
)
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}
