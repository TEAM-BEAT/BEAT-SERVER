package com.beat.domain.admin.application.dto;

import com.beat.domain.performance.domain.Performance;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;

import java.util.List;
import java.util.Optional;

public record CarouselFindAllResponse(
	List<CarouselFindResponse> carousels
) {
	public static CarouselFindAllResponse from(List<Promotion> promotions) {
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
		public static CarouselFindResponse from(Promotion promotion) {
			return new CarouselFindResponse(
				promotion.getId(),
				promotion.getCarouselNumber(),
				promotion.getPromotionPhoto(),
				promotion.isExternal(),
				promotion.getRedirectUrl(),
				Optional.ofNullable(promotion.getPerformance())
					.map(Performance::getId)
					.orElse(null)
			);
		}
	}
}