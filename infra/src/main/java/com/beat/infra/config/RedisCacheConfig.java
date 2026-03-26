package com.beat.infra.config;

import org.springframework.context.annotation.Configuration;

import com.beat.infra.InfraBaseConfig;

/**
 * Placeholder for future shared Redis cache policy.
 *
 * <p>Current Redis runtime wiring remains owned by Spring Boot auto-configuration
 * plus gateway-local Redis usage. When cross-module caching requirements appear,
 * this config is the intended bootstrap point for shared cache concerns such as
 * CacheManager, serializers, TTL policy, and cache namespace conventions.
 */
@Configuration(proxyBeanMethods = false)
public class RedisCacheConfig implements InfraBaseConfig {
}
