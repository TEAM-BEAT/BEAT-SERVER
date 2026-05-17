package com.beat.batch.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

import com.beat.batch.BatchApplication;

@SpringBootTest(classes = BatchApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
public abstract class AbstractBatchIntegrationTest {

	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.39")
		.withDatabaseName("beat_batch_test");

	static {
		mysql.start();
	}
}
