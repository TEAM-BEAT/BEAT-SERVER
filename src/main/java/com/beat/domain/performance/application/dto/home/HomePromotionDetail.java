package com.beat.domain.performance.application.dto.home;

import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;

public record HomePromotionDetail(Long promotionId, String promotionPhoto, Long performanceId, String redirectUrl,
								  boolean isExternal, CarouselNumber carouselNumber) {

	public static HomePromotionDetail from(Promotion promotion) {
		Long performanceId = null;

		if (promotion.getPerformance() != null) {
			performanceId = promotion.getPerformance().getId();
		}

		return new HomePromotionDetail(promotion.getId(), promotion.getPromotionPhoto(), performanceId,
			promotion.getRedirectUrl(), promotion.isExternal(), promotion.getCarouselNumber());
	}
}
