package com.beat.infra

import com.beat.infra.config.AsyncConfig
import com.beat.infra.config.ExternalClientConfig
import com.beat.infra.config.JpaConfig
import com.beat.infra.config.RedisCacheConfig

enum class InfraBaseConfigGroup(
    internal val configClass: Class<*>,
) {
    ASYNC(AsyncConfig::class.java),
    EXTERNAL_CLIENTS(ExternalClientConfig::class.java),
    JPA(JpaConfig::class.java),
    REDIS_CACHE(RedisCacheConfig::class.java),
}
