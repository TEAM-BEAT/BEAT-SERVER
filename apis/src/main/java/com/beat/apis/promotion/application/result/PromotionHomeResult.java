package com.beat.apis.promotion.application.result;

public record PromotionHomeResult(
	Long promotionId,
	String promotionPhoto,
	Long performanceId,
	String redirectUrl,
	boolean external,
	String carouselNumber,
	int carouselNumberOrder
) {

	public static PromotionHomeResult of(Long promotionId, String promotionPhoto, Long performanceId,
		String redirectUrl, boolean external, String carouselNumber, int carouselNumberOrder) {
		return new PromotionHomeResult(
			promotionId,
			promotionPhoto,
			performanceId,
			redirectUrl,
			external,
			carouselNumber,
			carouselNumberOrder
		);
	}
}
