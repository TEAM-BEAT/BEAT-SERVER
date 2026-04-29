package com.beat.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.scheduler.application.JobSchedulerService;
import com.beat.batch.support.AbstractBatchIntegrationTest;
import com.beat.contracts.schedule.ScheduleJobPort;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.schedule.repository.ScheduleRepository;

class BatchModuleContextBootTest extends AbstractBatchIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Environment environment;

	@Autowired
	private ScheduleJobPort scheduleJobPort;

	@Autowired
	private JobSchedulerService jobSchedulerService;

	@Test
	void contextLoadsWithSchedulerOwnerDisabledInTestProfile() {
		assertEquals("false", environment.getProperty("beat.scheduler.owner"));
		assertEquals(true, applicationContext.containsBean("taskScheduler"));
		assertEquals(1, applicationContext.getBeansOfType(JobSchedulerService.class).size());
		assertEquals(1, applicationContext.getBeansOfType(ScheduleJobPort.class).size());
		assertEquals(1, applicationContext.getBeansOfType(TaskScheduler.class).size());
		assertNotNull(scheduleJobPort);
		assertSame(jobSchedulerService, scheduleJobPort);
		assertEquals(false, ReflectionTestUtils.getField(jobSchedulerService, "schedulerOwner"));
		assertEquals(1, applicationContext.getBeansOfType(PromotionRepository.class).size());
		assertEquals(1, applicationContext.getBeansOfType(ScheduleRepository.class).size());
	}
}
