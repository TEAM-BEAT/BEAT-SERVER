package com.beat.batch.config

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
    ]
)
// @EnableInfraBaseConfig is backed by DeferredImportSelector, which IntelliJ Spring plugin
// cannot statically trace. This explicit import is an IDE breadcrumb only — runtime
// persistence bootstrap is owned by JpaConfig via @Import(InfraPersistenceConfig.class).
@Import(InfraPersistenceConfig::class)
class InfraConfig
