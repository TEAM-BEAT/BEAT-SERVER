package com.beat.infrastructure.support

import com.redis.testcontainers.RedisContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean

@TestConfiguration(proxyBeanMethods = false)
class RedisTestContainerConfig {

    @Bean
    @ServiceConnection
    fun redisContainer(): RedisContainer =
        RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG))
}
