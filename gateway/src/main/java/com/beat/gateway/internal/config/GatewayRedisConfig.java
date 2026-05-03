package com.beat.gateway.internal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import com.beat.gateway.jwt.internal.store.RefreshTokenRepository;

@Configuration(proxyBeanMethods = false)
@EnableRedisRepositories(basePackageClasses = RefreshTokenRepository.class)
public class GatewayRedisConfig {
}
