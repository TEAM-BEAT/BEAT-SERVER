package com.beat.batch.promotion.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.promotion.model.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.promotion.service.PromotionCarouselDomainService;
import com.beat.domain.promotion.service.PromotionEligibilityDomainService;
import com.beat.domain.schedule.model.Schedule;
import com.beat.domain.schedule.repository.ScheduleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionMaintenanceService {

	private final PromotionRepository promotionRepository;
	private final ScheduleRepository scheduleRepository;
	private final PromotionCarouselDomainService promotionCarouselDomainService;
	private final PromotionEligibilityDomainService promotionEligibilityDomainService;

	@Transactional
	public void checkAndDeleteInvalidPromotions() {
		List<Long> promotionIdsToDelete = promotionRepository.findAll().stream()
			.filter(this::isInvalidPromotion)
			.map(Promotion::getId)
			.toList();

		if (promotionIdsToDelete.isEmpty()) {
			return;
		}

		log.info("Deleting promotions: {}", promotionIdsToDelete);

		promotionRepository.deleteByPromotionIds(promotionIdsToDelete);
		reassignCarouselNumbers();
	}

	private boolean isInvalidPromotion(Promotion promotion) {
		if (promotion.getPerformanceId() == null) {
			return false;
		}

		List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(promotion.getPerformanceId());
		return !promotionEligibilityDomainService.isEligible(
			promotion,
			schedules.stream().map(Schedule::getPerformanceDate).toList(),
			LocalDate.now()
		);
	}

	private void reassignCarouselNumbers() {
		List<Promotion> remainingPromotions = promotionRepository.findAll();
		promotionRepository.saveAll(promotionCarouselDomainService.arrangeCarouselNumbers(remainingPromotions));
	}
}
