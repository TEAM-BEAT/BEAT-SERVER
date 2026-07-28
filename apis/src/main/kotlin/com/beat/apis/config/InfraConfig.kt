package com.beat.apis.config

import com.beat.infra.EnableInfraBaseConfig
import com.beat.infra.InfraBaseConfigGroup
import com.beat.infra.persistence.InfraPersistenceConfig
import com.beat.infra.redis.auth.AuthRedisConfig
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
// InfraPersistenceConfig is an IDE breadcrumb for JpaConfig's deferred import.
// AuthRedisConfig is an intentional runtime composition: only apis owns the Redis runtime dependency.
@Import(
    InfraPersistenceConfig::class,
    AuthRedisConfig::class,
)
class InfraConfig
