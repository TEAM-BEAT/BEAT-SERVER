package com.beat.admin.promotion.api.response;

import java.util.Map;

import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult;

public record CarouselPresignedUrlFindAllResponse(
	Map<String, String> carouselPresignedUrls
) {
	private static CarouselPresignedUrlFindAllResponse of(Map<String, String> carouselPresignedUrls) {
		return new CarouselPresignedUrlFindAllResponse(carouselPresignedUrls);
	}

	public static CarouselPresignedUrlFindAllResponse from(CarouselPresignedUrlsResult result) {
		return CarouselPresignedUrlFindAllResponse.of(result.carouselPresignedUrls());
	}
}
