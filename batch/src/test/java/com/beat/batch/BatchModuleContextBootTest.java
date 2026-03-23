package com.beat.batch;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(classes = BatchApplication.class)
@ActiveProfiles("test")
@Tag("integration")
class BatchModuleContextBootTest {

	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.39")
		.withDatabaseName("beat_batch_test");

	static {
		mysql.start();
	}

	@Test
	void contextLoads() {
	}
}
