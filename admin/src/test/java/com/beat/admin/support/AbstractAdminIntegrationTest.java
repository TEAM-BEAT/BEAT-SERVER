package com.beat.admin.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

import com.beat.admin.AdminApplication;

@SpringBootTest(classes = AdminApplication.class)
@ActiveProfiles("test")
@Tag("integration")
public abstract class AbstractAdminIntegrationTest {

	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.39")
		.withDatabaseName("beat_admin_test")
		.withCommand("--default-time-zone=+09:00");

	static {
		mysql.start();
	}
}
