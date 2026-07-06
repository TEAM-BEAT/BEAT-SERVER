package com.beat.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.support.AbstractBatchIntegrationTest;
import com.beat.batch.booking.job.TicketCleanupJob;
import com.beat.batch.promotion.job.PromotionMaintenanceJob;

@TestPropertySource(properties = "beat.scheduler.owner=true")
class BatchSchedulerOwnerBootTest extends AbstractBatchIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Environment environment;

	@Test
	void contextBootsWithSchedulerOwnerEnabled() {
		assertEquals("true", environment.getProperty("beat.scheduler.owner"));
		assertEquals(false, applicationContext.containsBean("taskScheduler"));
		assertEquals(true, applicationContext.containsBean("maintenanceTaskScheduler"));
		assertEquals(1, applicationContext.getBeansOfType(TaskScheduler.class).size());
		TicketCleanupJob ticketCleanupJob = applicationContext.getBean(TicketCleanupJob.class);
		PromotionMaintenanceJob promotionMaintenanceJob = applicationContext.getBean(PromotionMaintenanceJob.class);
		assertNotNull(ticketCleanupJob);
		assertNotNull(promotionMaintenanceJob);
		assertEquals(true, ReflectionTestUtils.getField(ticketCleanupJob, "schedulerOwner"));
		assertEquals(true, ReflectionTestUtils.getField(promotionMaintenanceJob, "schedulerOwner"));
	}
}
