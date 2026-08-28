package com.beat.apps.batch.support

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.mysql.MySQLContainer

@TestConfiguration(proxyBeanMethods = false)
class BeatBatchTestContainersConfig {
    @Bean
    @Primary
    fun testClock(): Clock =
        Clock.fixed(
            Instant.parse("2026-08-23T00:00:00Z"),
            ZoneId.of("Asia/Seoul"),
        )

    @Bean
    @ServiceConnection
    fun mysqlContainer(): MySQLContainer =
        MySQLContainer("mysql:8.4.11")
            .withDatabaseName("beat_batch_test")
            .withCommand("--default-time-zone=+09:00")
}
