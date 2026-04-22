package com.beat.apis.performance.application.dto.home;

import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;

public record HomePromotionDetail(Long promotionId, String promotionPhoto, Long performanceId, String redirectUrl,
								  boolean isExternal, CarouselNumber carouselNumber) {

	public static HomePromotionDetail from(Promotion promotion) {
		return new HomePromotionDetail(promotion.getId(), promotion.getPromotionPhoto(), promotion.getPerformanceId(),
			promotion.getRedirectUrl(), promotion.isExternal(), promotion.getCarouselNumber());
	}
}
