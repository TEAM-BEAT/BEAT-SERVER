package com.beat.apps.api.support

import com.redis.testcontainers.RedisContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.mysql.MySQLContainer
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@TestConfiguration(proxyBeanMethods = false)
class BeatTestContainersConfig {
    companion object {
        /** 앱 시간 고정 기준점. 회차 일정 등 DB 시간(CURRENT_TIMESTAMP) 연동 fixture는 실제 현재 기준 미래여야 한다. */
        val FIXED_NOW: LocalDateTime = LocalDateTime.of(2026, 8, 23, 9, 0)
    }

    @Bean
    @Primary
    fun testClock(): Clock = Clock.fixed(
        Instant.parse("2026-08-23T00:00:00Z"),
        ZoneId.of("Asia/Seoul"),
    )

    @Bean
    @ServiceConnection
    fun mysql(): MySQLContainer = MySQLContainer("mysql:8.4.11")
        .withDatabaseName("beat_apis_test")
        .withCommand("--default-time-zone=+09:00")

    @Bean
    @ServiceConnection
    fun redis(): RedisContainer = RedisContainer(
        RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG),
    )
}
