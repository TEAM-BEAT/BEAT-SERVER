package com.beat.domain.promotion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.domain.exception.DomainException;
import com.beat.domain.promotion.model.CarouselNumber;
import com.beat.domain.promotion.model.Promotion;
import com.beat.domain.promotion.exception.PromotionErrorCode;
import com.beat.domain.promotion.service.PromotionCarouselDomainService;
import com.beat.domain.promotion.service.PromotionEligibilityDomainService;

class PromotionDomainServiceTest {

	private final PromotionCarouselDomainService promotionCarouselDomainService = new PromotionCarouselDomainService();
	private final PromotionEligibilityDomainService promotionEligibilityDomainService =
		new PromotionEligibilityDomainService();
	private final LocalDate today = LocalDate.of(2026, 7, 16);

	@Test
	void validatesCarouselAssignmentsBeforeMutation() {
		assertTrue(promotionCarouselDomainService.hasValidCarouselAssignments(
			List.of(CarouselNumber.ONE, CarouselNumber.THREE)));
		assertFalse(promotionCarouselDomainService.hasValidCarouselAssignments(
			List.of(CarouselNumber.ONE, CarouselNumber.ONE)));
		assertFalse(promotionCarouselDomainService.hasValidCarouselAssignments(
			Arrays.asList(CarouselNumber.ONE, null)));
	}

	@Test
	void sortsAndCompactsCarouselNumbers() {
		Promotion fifth = promotion(1L, null, CarouselNumber.FIVE);
		Promotion second = promotion(2L, null, CarouselNumber.TWO);

		List<Promotion> arranged = promotionCarouselDomainService.arrangeCarouselNumbers(List.of(fifth, second));

		assertEquals(2L, arranged.get(0).getId());
		assertEquals(CarouselNumber.ONE, arranged.get(0).getCarouselNumber());
		assertEquals(1L, arranged.get(1).getId());
		assertEquals(CarouselNumber.TWO, arranged.get(1).getCarouselNumber());
	}

	@Test
	void rejectsMorePromotionsThanAvailableSlots() {
		List<Promotion> promotions = new ArrayList<>(Arrays.stream(CarouselNumber.values())
			.map(number -> promotion((long)number.ordinal(), null, number))
			.toList());
		promotions.add(promotion(99L, null, CarouselNumber.ONE));

		DomainException exception = assertThrows(DomainException.class,
			() -> promotionCarouselDomainService.arrangeCarouselNumbers(promotions));
		assertEquals(PromotionErrorCode.TOO_MANY_CAROUSEL_PROMOTIONS, exception.getErrorCode());
	}

	@Test
	void internalPromotionIsIneligibleWhenEveryScheduleIsPast() {
		Promotion promotion = promotion(1L, 1L, CarouselNumber.ONE);

		assertFalse(promotionEligibilityDomainService.isEligible(promotion,
			List.of(today.minusDays(2).atStartOfDay(), today.minusDays(1).atStartOfDay()), today));
	}

	@Test
	void internalPromotionRemainsEligibleForTodayOrFutureSchedule() {
		Promotion promotion = promotion(1L, 1L, CarouselNumber.ONE);

		assertTrue(promotionEligibilityDomainService.isEligible(promotion,
			List.of(today.minusDays(1).atStartOfDay(), today.atStartOfDay()), today));
	}

	@Test
	void promotionWithoutPerformanceOrSchedulesPreservesExistingEligibility() {
		Promotion external = promotion(1L, null, CarouselNumber.ONE);

		assertTrue(promotionEligibilityDomainService.isEligible(external,
			List.of(today.minusDays(1).atStartOfDay()), today));
		assertTrue(promotionEligibilityDomainService.isEligible(
			promotion(1L, 1L, CarouselNumber.ONE), List.of(), today));
	}

	private static Promotion promotion(Long id, Long performanceId, CarouselNumber carouselNumber) {
		return Promotion.rehydrate(
			id,
			"image",
			performanceId,
			"url",
			performanceId == null,
			carouselNumber
		);
	}
}
