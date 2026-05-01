package com.beat.admin.application.dto.result;

import com.beat.domain.promotion.domain.CarouselNumber;

public record AdminPromotionResult(
	Long promotionId,
	CarouselNumber carouselNumber,
	String newImageUrl,
	boolean isExternal,
	String redirectUrl,
	Long performanceId
) {
}
