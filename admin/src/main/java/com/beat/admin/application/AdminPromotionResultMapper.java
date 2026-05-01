package com.beat.admin.application;

import java.util.Comparator;

import com.beat.admin.application.dto.result.AdminPromotionResult;
import com.beat.domain.promotion.domain.Promotion;

final class AdminPromotionResultMapper {

	static final Comparator<Promotion> BY_CAROUSEL_NUMBER = Comparator.comparing(
		Promotion::getCarouselNumber,
		Comparator.comparingInt(Enum::ordinal)
	);

	private AdminPromotionResultMapper() {
	}

	static AdminPromotionResult toResult(Promotion promotion) {
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
