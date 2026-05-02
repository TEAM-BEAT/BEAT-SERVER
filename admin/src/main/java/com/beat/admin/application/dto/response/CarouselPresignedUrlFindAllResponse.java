package com.beat.admin.application.dto.response;

import java.util.Map;

import com.beat.contracts.storage.CarouselPresignedUrls;

public record CarouselPresignedUrlFindAllResponse(
	Map<String, String> carouselPresignedUrls
) {
	private static CarouselPresignedUrlFindAllResponse of(Map<String, String> carouselPresignedUrls) {
		return new CarouselPresignedUrlFindAllResponse(carouselPresignedUrls);
	}

	public static CarouselPresignedUrlFindAllResponse from(CarouselPresignedUrls response) {
		return CarouselPresignedUrlFindAllResponse.of(response.carouselPresignedUrls());
	}
}
