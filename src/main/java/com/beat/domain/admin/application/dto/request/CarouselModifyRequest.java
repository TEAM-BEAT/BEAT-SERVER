package com.beat.domain.admin.application.dto.request;

import java.util.List;

import com.beat.domain.promotion.domain.CarouselNumber;

public record CarouselModifyRequest(
	List<PromotionModifyRequest> carousels
) {
	public record PromotionModifyRequest(
		Long promotionId,
		CarouselNumber carouselNumber,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) {
	}
}