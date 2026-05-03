package com.beat.admin.config

import com.beat.infra.EnableInfraBaseConfig
import com.beat.infra.InfraBaseConfigGroup
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableInfraBaseConfig(
    value = [
        InfraBaseConfigGroup.JPA,
        InfraBaseConfigGroup.EXTERNAL_CLIENTS,
    ]
)
class InfraConfig
