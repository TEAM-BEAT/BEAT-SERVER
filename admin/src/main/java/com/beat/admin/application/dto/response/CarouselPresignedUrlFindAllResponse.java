package com.beat.admin.application.dto.response;

import com.beat.contracts.storage.CarouselPresignedUrls;
import java.util.Map;

public record CarouselPresignedUrlFindAllResponse(
	Map<String, String> carouselPresignedUrls
) {

	public static CarouselPresignedUrlFindAllResponse from(CarouselPresignedUrls response) {
		return new CarouselPresignedUrlFindAllResponse(response.carouselPresignedUrls());
	}
}
