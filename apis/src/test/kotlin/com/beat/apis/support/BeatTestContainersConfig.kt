package com.beat.apis.support

import com.redis.testcontainers.RedisContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.mysql.MySQLContainer

@TestConfiguration(proxyBeanMethods = false)
class BeatTestContainersConfig {
    @Bean
    @ServiceConnection
    fun mysql(): MySQLContainer = MySQLContainer("mysql:8.0.39")
        .withDatabaseName("beat_apis_test")
        .withCommand("--default-time-zone=+09:00")

    @Bean
    @ServiceConnection
    fun redis(): RedisContainer = RedisContainer(
        RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG),
    )
}
