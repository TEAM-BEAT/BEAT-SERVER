package com.beat.batch.promotion.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.beat.batch.promotion.facade.PromotionMaintenanceFacade;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromotionMaintenanceJob {

	private final PromotionMaintenanceFacade promotionMaintenanceFacade;

	@Value("${beat.scheduler.owner:false}")
	private boolean schedulerOwner;

	@Scheduled(cron = "1 0 0 * * ?", scheduler = "maintenanceTaskScheduler")
	public void checkAndDeleteInvalidPromotions() {
		if (!schedulerOwner) {
			return;
		}

		promotionMaintenanceFacade.checkAndDeleteInvalidPromotions();
	}
}
