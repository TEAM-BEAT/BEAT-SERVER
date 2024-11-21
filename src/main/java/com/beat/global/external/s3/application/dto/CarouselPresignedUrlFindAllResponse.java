package com.beat.global.external.s3.application.dto;

import java.util.Map;

public record CarouselPresignedUrlFindAllResponse(
	Map<String, String> carouselPresignedUrls
) {
	public static CarouselPresignedUrlFindAllResponse from(Map<String, String> carouselPresignedUrls) {
		return new CarouselPresignedUrlFindAllResponse(carouselPresignedUrls);
	}
}
