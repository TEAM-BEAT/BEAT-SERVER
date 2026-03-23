package com.beat.domain.promotion.port.in;

import com.beat.domain.promotion.domain.CarouselNumber;

public record PromotionModifyCommand(
	Long promotionId,
	CarouselNumber carouselNumber,
	String newImageUrl,
	boolean isExternal,
	String redirectUrl,
	Long performanceId
) {
}
