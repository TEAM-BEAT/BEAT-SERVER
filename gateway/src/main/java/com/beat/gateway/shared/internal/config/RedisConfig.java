package com.beat.gateway.shared.internal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import com.beat.gateway.guest.internal.store.GuestSessionRepository;
import com.beat.gateway.refreshtoken.internal.store.RefreshTokenRepository;

@Configuration(proxyBeanMethods = false)
@EnableRedisRepositories(basePackageClasses = {RefreshTokenRepository.class, GuestSessionRepository.class})
public class RedisConfig {
}
