package com.beat.apis.config

import com.beat.infra.EnableInfraBaseConfig
import com.beat.infra.InfraBaseConfigGroup
import com.beat.infra.persistence.InfraPersistenceConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@EnableInfraBaseConfig(
    value = [
        InfraBaseConfigGroup.JPA,
        InfraBaseConfigGroup.ASYNC,
        InfraBaseConfigGroup.EXTERNAL_CLIENTS,
    ]
)
// IDE static-analysis breadcrumb; runtime persistence import is still owned by JpaConfig.
@Import(InfraPersistenceConfig::class)
class InfraConfig
