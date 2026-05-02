package com.beat.admin.application.dto.response;

import java.util.List;

import com.beat.admin.application.dto.result.AdminPromotionResults;
import com.beat.admin.application.dto.result.AdminPromotionResults.AdminPromotionResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CarouselFindAllResponse(
	@JsonProperty("carousels")
	List<CarouselFindResponse> carouselResponses
) {
	private static CarouselFindAllResponse fromResponses(List<CarouselFindResponse> carouselResponses) {
		return new CarouselFindAllResponse(carouselResponses);
	}

	public static CarouselFindAllResponse from(AdminPromotionResults promotionResults) {
		List<CarouselFindResponse> carouselResponses = promotionResults.promotionResults().stream()
			.map(CarouselFindResponse::from)
			.toList();
		return CarouselFindAllResponse.fromResponses(carouselResponses);
	}

	public record CarouselFindResponse(
		Long promotionId,
		String carouselNumber,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) {
		private static CarouselFindResponse of(Long promotionId, String carouselNumber, String newImageUrl,
			boolean isExternal, String redirectUrl, Long performanceId) {
			return new CarouselFindResponse(promotionId, carouselNumber, newImageUrl, isExternal, redirectUrl,
				performanceId);
		}

		public static CarouselFindResponse from(AdminPromotionResult promotionResult) {
			return CarouselFindResponse.of(
				promotionResult.promotionId(),
				promotionResult.carouselNumber(),
				promotionResult.newImageUrl(),
				promotionResult.isExternal(),
				promotionResult.redirectUrl(),
				promotionResult.performanceId()
			);
		}
	}
}
