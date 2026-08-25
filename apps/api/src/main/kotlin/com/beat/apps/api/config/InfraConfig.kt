package com.beat.apps.api.config

import com.beat.infrastructure.EnableInfraBaseConfig
import com.beat.infrastructure.InfraBaseConfigGroup
import com.beat.infrastructure.persistence.InfraPersistenceConfig
import com.beat.infrastructure.redis.auth.AuthRedisConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@EnableInfraBaseConfig(
    value =
        [
            InfraBaseConfigGroup.JPA,
            InfraBaseConfigGroup.ASYNC,
            InfraBaseConfigGroup.EXTERNAL_CLIENTS,
        ]
)
// InfraPersistenceConfig is an IDE breadcrumb for JpaConfig's deferred import.
// AuthRedisConfig is an intentional runtime composition: only apis owns the Redis runtime
// dependency.
@Import(
    InfraPersistenceConfig::class,
    AuthRedisConfig::class,
)
class InfraConfig
