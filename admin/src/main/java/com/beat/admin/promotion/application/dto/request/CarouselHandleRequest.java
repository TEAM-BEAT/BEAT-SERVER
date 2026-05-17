package com.beat.admin.promotion.application.dto.request;

import java.util.List;

public record CarouselHandleRequest(
	List<PromotionHandleRequest> carousels
) {

	public record PromotionModifyRequest(
		Long promotionId,
		AdminCarouselNumber carouselNumber,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) implements PromotionHandleRequest {
	}

	public record PromotionGenerateRequest(
		AdminCarouselNumber carouselNumber,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) implements PromotionHandleRequest {
	}
}
