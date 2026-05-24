package com.beat.admin.promotion.application.dto.result;

import java.util.List;

import com.beat.global.support.jackson.CdnImageUrl;

public record AdminPromotionResults(
	List<AdminPromotionResult> promotionResults
) {

	public AdminPromotionResults {
		promotionResults = List.copyOf(promotionResults);
	}

	public static AdminPromotionResults from(List<AdminPromotionResult> promotionResults) {
		return new AdminPromotionResults(promotionResults);
	}

	public record AdminPromotionResult(
		Long promotionId,
		String carouselNumber,
		@CdnImageUrl String newImageUrl,
		boolean isExternal,
		String redirectUrl,
		Long performanceId
	) {
		public static AdminPromotionResult of(Long promotionId, String carouselNumber, String newImageUrl,
			boolean isExternal, String redirectUrl, Long performanceId) {
			return new AdminPromotionResult(promotionId, carouselNumber, newImageUrl, isExternal, redirectUrl,
				performanceId);
		}
	}
}
