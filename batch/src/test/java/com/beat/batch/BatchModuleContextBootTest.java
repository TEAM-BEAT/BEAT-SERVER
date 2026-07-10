package com.beat.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.booking.job.TicketCleanupJob;
import com.beat.batch.promotion.job.PromotionMaintenanceJob;
import com.beat.batch.support.AbstractBatchIntegrationTest;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.schedule.repository.ScheduleRepository;

class BatchModuleContextBootTest extends AbstractBatchIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Environment environment;

	@Test
	void contextLoadsWithSchedulerOwnerDisabledInTestProfile() {
		assertEquals("false", environment.getProperty("beat.scheduler.owner"));
		assertEquals(false, applicationContext.containsBean("taskScheduler"));
		assertEquals(true, applicationContext.containsBean("maintenanceTaskScheduler"));
		assertEquals(1, applicationContext.getBeansOfType(TaskScheduler.class).size());
		assertEquals(false,
			ReflectionTestUtils.getField(applicationContext.getBean(TicketCleanupJob.class), "schedulerOwner"));
		assertEquals(false,
			ReflectionTestUtils.getField(applicationContext.getBean(PromotionMaintenanceJob.class), "schedulerOwner"));
		assertEquals(1, applicationContext.getBeansOfType(PromotionRepository.class).size());
		assertEquals(1, applicationContext.getBeansOfType(ScheduleRepository.class).size());
	}
}
