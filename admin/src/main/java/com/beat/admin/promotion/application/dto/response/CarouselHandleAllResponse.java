package com.beat.admin.promotion.application.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.beat.admin.promotion.application.dto.result.AdminPromotionResults;
import com.beat.admin.promotion.application.dto.result.AdminPromotionResults.AdminPromotionResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CarouselHandleAllResponse(
	@JsonProperty("modifiedPromotions")
	List<PromotionResponse> modifiedPromotionResponses
) {

	private static CarouselHandleAllResponse fromResponses(List<PromotionResponse> modifiedPromotionResponses) {
		return new CarouselHandleAllResponse(modifiedPromotionResponses);
	}

	public static CarouselHandleAllResponse from(AdminPromotionResults promotionResults) {
		List<PromotionResponse> modifiedPromotionResponses = promotionResults.promotionResults().stream()
			.map(PromotionResponse::from)
			.collect(Collectors.toList());
		return CarouselHandleAllResponse.fromResponses(modifiedPromotionResponses);
	}

	public record PromotionResponse(
		Long promotionId,
		String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		String carouselNumber
	) {
		private static PromotionResponse of(Long promotionId, String newImageUrl, boolean isExternal,
			String redirectUrl, String carouselNumber) {
			return new PromotionResponse(promotionId, newImageUrl, isExternal, redirectUrl, carouselNumber);
		}

		public static PromotionResponse from(AdminPromotionResult promotionResult) {
			return PromotionResponse.of(
				promotionResult.promotionId(),
				promotionResult.newImageUrl(),
				promotionResult.isExternal(),
				promotionResult.redirectUrl(),
				promotionResult.carouselNumber()
			);
		}
	}
}
