package com.beat.admin.promotion.api.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult;
import com.beat.contracts.storage.CarouselPresignedUpload;

class CarouselPresignedUrlFindAllResponseTest {

	@Test
	void responsePreservesLegacyUrlsAndAddsExplicitUploadMetadata() {
		CarouselPresignedUrlFindAllResponse response = new CarouselPresignedUrlFindAllResponse(
			new CarouselPresignedUrlsResult(Map.of("carousel.png",
				CarouselPresignedUpload.of("signed-upload-url", "dev/carousel/carousel.png")))
		);

		assertEquals(Map.of("carousel.png", "signed-upload-url"), response.getCarouselPresignedUrls());
		assertEquals("signed-upload-url", response.getCarouselPresignedUploads().get("carousel.png").getUploadUrl());
		assertEquals("dev/carousel/carousel.png", response.getCarouselPresignedUploads().get("carousel.png").getImageKey());
	}
}
