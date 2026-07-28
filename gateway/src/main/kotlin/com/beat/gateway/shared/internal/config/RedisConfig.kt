package com.beat.gateway.shared.internal.config

import com.beat.gateway.guest.internal.store.GuestSessionRepository
import com.beat.gateway.refreshtoken.internal.store.RefreshTokenRepository
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

@Configuration(proxyBeanMethods = false)
@EnableRedisRepositories(basePackageClasses = [RefreshTokenRepository::class, GuestSessionRepository::class])
class RedisConfig
