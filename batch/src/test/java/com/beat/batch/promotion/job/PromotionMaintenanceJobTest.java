package com.beat.batch.promotion.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.promotion.application.PromotionMaintenanceService;

class PromotionMaintenanceJobTest {

	@Test
	void scheduledPromotionMaintenanceKeepsCronContract() throws NoSuchMethodException {
		Scheduled scheduled = PromotionMaintenanceJob.class
			.getDeclaredMethod("checkAndDeleteInvalidPromotions")
			.getAnnotation(Scheduled.class);

		assertEquals("1 0 0 * * ?", scheduled.cron());
	}

	@Test
	void scheduledPromotionMaintenanceDelegatesWhenRuntimeOwnsScheduler() {
		PromotionMaintenanceService promotionMaintenanceService = mock(PromotionMaintenanceService.class);
		PromotionMaintenanceJob promotionMaintenanceJob = new PromotionMaintenanceJob(promotionMaintenanceService);
		ReflectionTestUtils.setField(promotionMaintenanceJob, "schedulerOwner", true);

		promotionMaintenanceJob.checkAndDeleteInvalidPromotions();

		verify(promotionMaintenanceService).checkAndDeleteInvalidPromotions();
	}

	@Test
	void scheduledPromotionMaintenanceSkipsWhenRuntimeIsNotSchedulerOwner() {
		PromotionMaintenanceService promotionMaintenanceService = mock(PromotionMaintenanceService.class);
		PromotionMaintenanceJob promotionMaintenanceJob = new PromotionMaintenanceJob(promotionMaintenanceService);
		ReflectionTestUtils.setField(promotionMaintenanceJob, "schedulerOwner", false);

		promotionMaintenanceJob.checkAndDeleteInvalidPromotions();

		verifyNoInteractions(promotionMaintenanceService);
	}
}
