package com.beat.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.support.AbstractBatchIntegrationTest;
import com.beat.contracts.schedule.ScheduleJobPort;
import com.beat.global.common.scheduler.application.JobSchedulerService;

@TestPropertySource(properties = "beat.scheduler.owner=true")
class BatchSchedulerOwnerBootTest extends AbstractBatchIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Environment environment;

	@Autowired
	private ScheduleJobPort scheduleJobPort;

	@Autowired
	private JobSchedulerService jobSchedulerService;

	@Test
	void contextBootsWithSchedulerOwnerEnabled() {
		assertEquals("true", environment.getProperty("beat.scheduler.owner"));
		assertEquals(1, applicationContext.getBeansOfType(JobSchedulerService.class).size());
		assertEquals(1, applicationContext.getBeansOfType(ScheduleJobPort.class).size());
		assertNotNull(scheduleJobPort);
		assertInstanceOf(JobSchedulerService.class, scheduleJobPort);
		assertSame(jobSchedulerService, scheduleJobPort);
		assertEquals(true, ReflectionTestUtils.getField(jobSchedulerService, "schedulerOwner"));
	}
}
