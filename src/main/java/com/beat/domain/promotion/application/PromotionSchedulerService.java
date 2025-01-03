package com.beat.domain.promotion.application;

import com.beat.domain.performance.domain.Performance;
import com.beat.domain.promotion.dao.PromotionRepository;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.schedule.application.ScheduleService;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionSchedulerService {

	private final PromotionRepository promotionRepository;
	private final ScheduleRepository scheduleRepository;
	private final ScheduleService scheduleService;

	@Scheduled(cron = "1 0 0 * * ?")
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
		return Optional.ofNullable(promotion.getPerformance())
			.map(performance -> {
				List<Schedule> schedules = scheduleRepository.findByPerformanceId(performance.getId());
				return scheduleService.getMinDueDate(schedules) < 0;
			})
			.orElse(false);
	}

	private void reassignCarouselNumbers() {
		List<Promotion> remainingPromotions = promotionRepository.findAll();
		remainingPromotions.sort(Comparator.comparing(promotion -> promotion.getCarouselNumber().getNumber()));

		List<CarouselNumber> carouselNumbers = Arrays.asList(CarouselNumber.values());
		for (int i = 0; i < remainingPromotions.size(); i++) {
			Promotion promotion = remainingPromotions.get(i);
			promotion.updateCarouselNumber(carouselNumbers.get(i));
		}

		promotionRepository.saveAll(remainingPromotions);
	}

}