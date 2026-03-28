package com.beat.batch.promotion.application;

import com.beat.domain.promotion.dao.PromotionRepository;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionSchedulerService {

	private final PromotionRepository promotionRepository;
	private final ScheduleRepository scheduleRepository;

	@Value("${beat.scheduler.owner:false}")
	private boolean schedulerOwner;

	@Scheduled(cron = "1 0 0 * * ?")
	@Transactional
	public void checkAndDeleteInvalidPromotions() {
		if (!schedulerOwner) {
			return;
		}

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
				List<Schedule> schedules = scheduleRepository.findAllByPerformanceId(performance.getId());
				return getMinDueDate(schedules) < 0;
			})
			.orElse(false);
	}

	private int getMinDueDate(List<Schedule> schedules) {
		OptionalInt minPositiveDueDate = schedules.stream()
			.mapToInt(schedule -> (int)ChronoUnit.DAYS.between(LocalDate.now(),
				schedule.getPerformanceDate().toLocalDate()))
			.filter(dueDate -> dueDate >= 0)
			.min();

		if (minPositiveDueDate.isPresent()) {
			return minPositiveDueDate.getAsInt();
		}
		return schedules.stream()
			.mapToInt(schedule -> (int)ChronoUnit.DAYS.between(LocalDate.now(),
				schedule.getPerformanceDate().toLocalDate()))
			.min()
			.orElse(Integer.MAX_VALUE);
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
