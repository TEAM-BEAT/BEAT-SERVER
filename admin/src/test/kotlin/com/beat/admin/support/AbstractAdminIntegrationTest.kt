package com.beat.admin.support

import com.beat.admin.AdminApplication
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.mysql.MySQLContainer

@SpringBootTest(classes = [AdminApplication::class])
@ActiveProfiles("test")
@Tag("integration")
abstract class AbstractAdminIntegrationTest {

    companion object {
        @ServiceConnection
        @JvmStatic
        val mysql: MySQLContainer = MySQLContainer("mysql:8.0.39")
            .withDatabaseName("beat_admin_test")
            .withCommand("--default-time-zone=+09:00")
            .apply { start() }
    }
}
