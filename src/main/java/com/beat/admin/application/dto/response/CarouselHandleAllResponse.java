package com.beat.admin.application.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.beat.domain.promotion.domain.Promotion;

public record CarouselHandleAllResponse(
	List<PromotionResponse> modifiedPromotions
) {

	public static CarouselHandleAllResponse from(List<Promotion> promotions) {
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
		public static PromotionResponse from(Promotion promotion) {
			return new PromotionResponse(
				promotion.getId(),
				promotion.getPromotionPhoto(),
				promotion.isExternal(),
				promotion.getRedirectUrl(),
				promotion.getCarouselNumber().name()
			);
		}
	}
}