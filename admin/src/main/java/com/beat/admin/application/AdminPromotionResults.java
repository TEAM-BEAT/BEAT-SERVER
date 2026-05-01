package com.beat.admin.application;

import java.util.Comparator;
import java.util.List;

import com.beat.admin.application.dto.result.AdminPromotionResult;
import com.beat.domain.promotion.domain.Promotion;

final class AdminPromotionResults {

	private static final Comparator<Promotion> BY_CAROUSEL_NUMBER = Comparator.comparing(
		Promotion::getCarouselNumber,
		Comparator.comparingInt(Enum::ordinal)
	);

	private AdminPromotionResults() {
	}

	static List<AdminPromotionResult> fromSortedByCarouselNumber(List<Promotion> promotions) {
		return promotions.stream()
			.sorted(BY_CAROUSEL_NUMBER)
			.map(AdminPromotionResults::toResult)
			.toList();
	}

	private static AdminPromotionResult toResult(Promotion promotion) {
		return new AdminPromotionResult(
			promotion.getId(),
			promotion.getCarouselNumber(),
			promotion.getPromotionPhoto(),
			promotion.isExternal(),
			promotion.getRedirectUrl(),
			promotion.getPerformanceId()
		);
	}
}
