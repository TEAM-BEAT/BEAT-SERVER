package com.beat.admin.application.dto.request;

import java.util.List;

import com.beat.domain.promotion.domain.CarouselNumber;

public record CarouselHandleRequest(
	List<PromotionHandleRequest> carousels
) {

	public record PromotionModifyRequest(
		Long promotionId,
		CarouselNumber carouselNumber,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) implements PromotionHandleRequest {
	}

	public record PromotionGenerateRequest(
		CarouselNumber carouselNumber,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) implements PromotionHandleRequest {
	}
}