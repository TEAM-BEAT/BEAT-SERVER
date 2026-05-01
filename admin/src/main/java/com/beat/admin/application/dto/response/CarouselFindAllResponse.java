package com.beat.admin.application.dto.response;

import java.util.List;

import com.beat.admin.application.dto.result.AdminPromotionResult;
import com.beat.domain.promotion.domain.CarouselNumber;

public record CarouselFindAllResponse(
	List<CarouselFindResponse> carousels
) {
	public static CarouselFindAllResponse from(List<AdminPromotionResult> promotions) {
		List<CarouselFindResponse> responses = promotions.stream()
			.map(CarouselFindResponse::from)
			.toList();
		return new CarouselFindAllResponse(responses);
	}

	public record CarouselFindResponse(
		Long promotionId,
		CarouselNumber carouselNumber,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) {
		public static CarouselFindResponse from(AdminPromotionResult promotion) {
			return new CarouselFindResponse(
				promotion.promotionId(),
				promotion.carouselNumber(),
				promotion.newImageUrl(),
				promotion.isExternal(),
				promotion.redirectUrl(),
				promotion.performanceId()
			);
		}
	}
}
