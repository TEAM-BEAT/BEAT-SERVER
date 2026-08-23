package com.beat.infrastructure.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.mysql.MySQLContainer

@TestConfiguration(proxyBeanMethods = false)
class MySqlTestContainerConfig {

    @Bean
    @ServiceConnection
    fun mysqlContainer(): MySQLContainer =
        MySQLContainer("mysql:8.0.39")
            .withDatabaseName("beat_apis_test")
            .withCommand("--default-time-zone=+09:00")
}
