package com.beat.batch;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;

import com.beat.contracts.schedule.ScheduleJobPort;
import com.beat.global.common.scheduler.application.JobSchedulerService;

@SpringBootTest(classes = BatchApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "beat.scheduler.owner=true")
@Tag("integration")
class BatchSchedulerOwnerBootTest {

	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.39")
		.withDatabaseName("beat_batch_test");

	static {
		mysql.start();
	}

	@Autowired
	private ScheduleJobPort scheduleJobPort;

	@Test
	void contextBootsWithSchedulerOwnerEnabled() {
		assertNotNull(scheduleJobPort);
		assertInstanceOf(JobSchedulerService.class, scheduleJobPort);
	}
}
