package com.beat.admin.application.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.beat.admin.application.dto.result.AdminPromotionResult;

public record CarouselHandleAllResponse(
	List<PromotionResponse> modifiedPromotions
) {

	public static CarouselHandleAllResponse from(List<AdminPromotionResult> promotions) {
		List<PromotionResponse> modifiedPromotions = promotions.stream()
			.map(PromotionResponse::from)
			.collect(Collectors.toList());
		return new CarouselHandleAllResponse(modifiedPromotions);
	}

	public record PromotionResponse(
		Long promotionId,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		String carouselNumber
	) {
		public static PromotionResponse from(AdminPromotionResult promotion) {
			return new PromotionResponse(
				promotion.promotionId(),
				promotion.newImageUrl(),
				promotion.isExternal(),
				promotion.redirectUrl(),
				promotion.carouselNumber().name()
			);
		}
	}
}
