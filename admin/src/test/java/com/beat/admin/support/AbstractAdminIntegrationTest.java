package com.beat.admin.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

import com.beat.admin.AdminApplication;
import com.redis.testcontainers.RedisContainer;

@SpringBootTest(classes = AdminApplication.class)
@ActiveProfiles("test")
@Tag("integration")
public abstract class AbstractAdminIntegrationTest {

	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.39")
		.withDatabaseName("beat_admin_test");

	@ServiceConnection
	static RedisContainer redis =
		new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

	static {
		mysql.start();
		redis.start();
	}
}
