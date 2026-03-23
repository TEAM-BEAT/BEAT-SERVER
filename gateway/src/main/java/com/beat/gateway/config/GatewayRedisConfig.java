package com.beat.gateway.config;

import com.beat.gateway.jwt.store.RefreshTokenRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration(proxyBeanMethods = false)
@EnableRedisRepositories(basePackageClasses = RefreshTokenRepository.class)
public class GatewayRedisConfig {
}
