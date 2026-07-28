package com.beat.infra.config;

import org.springframework.context.annotation.Configuration;

import com.beat.infra.InfraBaseConfig;

/**
 * Placeholder for future shared Redis cache policy.
 *
 * <p>Auth state Redis wiring is selected independently through
 * {@link com.beat.infra.redis.auth.AuthRedisConfig}.
 * When cross-module caching requirements appear,
 * this config is the intended bootstrap point for shared cache concerns such as
 * CacheManager, serializers, TTL policy, and cache namespace conventions.
 */
@Configuration(proxyBeanMethods = false)
public class RedisCacheConfig implements InfraBaseConfig {
}
