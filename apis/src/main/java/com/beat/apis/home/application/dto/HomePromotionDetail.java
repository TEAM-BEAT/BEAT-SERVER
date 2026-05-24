package com.beat.apis.home.application.dto;

import com.beat.global.support.jackson.CdnImageUrl;

public record HomePromotionDetail(
	Long promotionId,
	@CdnImageUrl String promotionPhoto,
	Long performanceId,
	String redirectUrl,
	boolean isExternal,
	String carouselNumber
) {

	public static HomePromotionDetail of(Long promotionId, String promotionPhoto, Long performanceId,
		String redirectUrl,
		boolean isExternal, String carouselNumber) {
		return new HomePromotionDetail(promotionId, promotionPhoto, performanceId, redirectUrl, isExternal,
			carouselNumber);
	}
}
