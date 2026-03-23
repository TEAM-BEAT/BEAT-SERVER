package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.beat.contracts.schedule.ScheduleJobPort;
import com.beat.apis.support.AbstractIntegrationTest;

@Tag("integration")
class ApisModuleContextBootTest extends AbstractIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ScheduleJobPort scheduleJobPort;

	@Test
	void contextLoadsWithModuleLocalNonOwnerScheduleJobPort() {
		assertNotNull(scheduleJobPort);
		assertEquals(1, applicationContext.getBeansOfType(ScheduleJobPort.class).size());
		assertFalse(applicationContext.containsBean("jobSchedulerService"));
		assertFalse(scheduleJobPort.getClass().getName().contains("JobSchedulerService"));
	}
}
