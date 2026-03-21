package com.beat.admin.config

import com.beat.infra.EnableInfraBaseConfig
import com.beat.infra.InfraBaseConfigGroup
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableInfraBaseConfig(
    value = [
        InfraBaseConfigGroup.JPA,
        InfraBaseConfigGroup.QUERY_DSL,
        InfraBaseConfigGroup.REDIS,
        InfraBaseConfigGroup.ASYNC,
    ]
)
class InfraConfig
