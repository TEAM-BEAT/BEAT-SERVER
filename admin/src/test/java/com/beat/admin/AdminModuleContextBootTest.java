package com.beat.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.beat.admin.port.in.AdminUseCase;
import com.beat.admin.support.AbstractAdminIntegrationTest;
import com.beat.contracts.schedule.ScheduleJobPort;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.domain.member.port.in.MemberUseCase;
import com.beat.domain.performance.port.in.PerformanceUseCase;
import com.beat.domain.promotion.port.in.PromotionUseCase;
import com.beat.domain.user.port.in.UserUseCase;

class AdminModuleContextBootTest extends AbstractAdminIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@MockitoBean
	private AdminUseCase adminUseCase;

	@MockitoBean
	private PromotionUseCase promotionUseCase;

	@MockitoBean
	private PerformanceUseCase performanceUseCase;

	@MockitoBean
	private FileStoragePort fileStoragePort;

	@MockitoBean
	private MemberUseCase memberUseCase;

	@MockitoBean
	private UserUseCase userUseCase;

	@Test
	void contextLoads() {
		assertFalse(applicationContext.containsBean("jobSchedulerService"));
		assertTrue(applicationContext.getBeansOfType(ScheduleJobPort.class).isEmpty());
		assertTrue(applicationContext.getBeansOfType(TaskScheduler.class).isEmpty());
	}
}
