package com.beat.infrastructure

import com.beat.infrastructure.config.AsyncConfig
import com.beat.infrastructure.config.ExternalClientConfig
import com.beat.infrastructure.config.JpaConfig
import com.beat.infrastructure.config.RedisCacheConfig

enum class InfraBaseConfigGroup(
    internal val configClass: Class<*>,
) {
    ASYNC(AsyncConfig::class.java),
    EXTERNAL_CLIENTS(ExternalClientConfig::class.java),
    JPA(JpaConfig::class.java),
    REDIS_CACHE(RedisCacheConfig::class.java),
}
