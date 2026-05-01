package com.beat.batch.promotion.job;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.promotion.application.PromotionSchedulerService;

class PromotionMaintenanceJobTest {

	@Test
	void scheduledPromotionMaintenanceDelegatesWhenRuntimeOwnsScheduler() {
		PromotionSchedulerService promotionSchedulerService = mock(PromotionSchedulerService.class);
		PromotionMaintenanceJob promotionMaintenanceJob = new PromotionMaintenanceJob(promotionSchedulerService);
		ReflectionTestUtils.setField(promotionMaintenanceJob, "schedulerOwner", true);

		promotionMaintenanceJob.checkAndDeleteInvalidPromotions();

		verify(promotionSchedulerService).checkAndDeleteInvalidPromotions();
	}

	@Test
	void scheduledPromotionMaintenanceSkipsWhenRuntimeIsNotSchedulerOwner() {
		PromotionSchedulerService promotionSchedulerService = mock(PromotionSchedulerService.class);
		PromotionMaintenanceJob promotionMaintenanceJob = new PromotionMaintenanceJob(promotionSchedulerService);
		ReflectionTestUtils.setField(promotionMaintenanceJob, "schedulerOwner", false);

		promotionMaintenanceJob.checkAndDeleteInvalidPromotions();

		verifyNoInteractions(promotionSchedulerService);
	}
}
