package com.beat.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.support.AbstractBatchIntegrationTest;
import com.beat.contracts.schedule.ScheduleBookingCloseJobPort;
import com.beat.batch.scheduler.application.JobSchedulerService;

@TestPropertySource(properties = "beat.scheduler.owner=true")
class BatchSchedulerOwnerBootTest extends AbstractBatchIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Environment environment;

	@Autowired
	private ScheduleBookingCloseJobPort scheduleBookingCloseJobPort;

	@Autowired
	private JobSchedulerService jobSchedulerService;

	@Test
	void contextBootsWithSchedulerOwnerEnabled() {
		assertEquals("true", environment.getProperty("beat.scheduler.owner"));
		assertEquals(true, applicationContext.containsBean("taskScheduler"));
		assertEquals(1, applicationContext.getBeansOfType(JobSchedulerService.class).size());
		assertEquals(1, applicationContext.getBeansOfType(ScheduleBookingCloseJobPort.class).size());
		assertEquals(1, applicationContext.getBeansOfType(TaskScheduler.class).size());
		assertNotNull(scheduleBookingCloseJobPort);
		assertInstanceOf(JobSchedulerService.class, scheduleBookingCloseJobPort);
		assertSame(jobSchedulerService, scheduleBookingCloseJobPort);
		assertEquals(true, ReflectionTestUtils.getField(jobSchedulerService, "schedulerOwner"));
	}
}
