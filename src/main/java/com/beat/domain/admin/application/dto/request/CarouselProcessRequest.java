package com.beat.domain.admin.application.dto.request;

import java.util.List;

import com.beat.domain.promotion.domain.CarouselNumber;

public record CarouselProcessRequest(
	List<PromotionHandleRequest> carousels
) {
	public record PromotionModifyRequest(
		Long promotionId,
		CarouselNumber carouselNumber,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) implements PromotionHandleRequest {}

	public record PromotionGenerateRequest(
		CarouselNumber carouselNumber,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) implements PromotionHandleRequest {}

	public sealed interface PromotionHandleRequest permits PromotionModifyRequest, PromotionGenerateRequest {
		CarouselNumber carouselNumber();
	}
}