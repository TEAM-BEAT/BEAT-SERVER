package com.beat.apis.performance.application.dto.home;

public record HomePromotionDetail(Long promotionId, String promotionPhoto, Long performanceId, String redirectUrl,
								  boolean isExternal, String carouselNumber) {

	public static HomePromotionDetail of(Long promotionId, String promotionPhoto, Long performanceId, String redirectUrl,
		boolean isExternal, String carouselNumber) {
		return new HomePromotionDetail(promotionId, promotionPhoto, performanceId, redirectUrl, isExternal, carouselNumber);
	}
}
