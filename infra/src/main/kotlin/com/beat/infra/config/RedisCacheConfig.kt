package com.beat.infra.config

import com.beat.infra.InfraBaseConfig
import org.springframework.context.annotation.Configuration

/**
 * Placeholder for future shared Redis cache policy.
 *
 * Auth state Redis wiring is selected independently through
 * [com.beat.infra.redis.auth.AuthRedisConfig]. When cross-module caching
 * requirements appear, this config is the intended bootstrap point for shared
 * cache concerns such as CacheManager, serializers, TTL policy, and cache
 * namespace conventions.
 */
@Configuration(proxyBeanMethods = false)
class RedisCacheConfig : InfraBaseConfig
