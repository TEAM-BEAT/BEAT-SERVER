package com.beat.admin.promotion.application.result;

import java.util.Map;

import com.beat.contracts.storage.CarouselPresignedUpload;

public final class AdminPromotionPresignedUrlResults {

	private AdminPromotionPresignedUrlResults() {
	}

	public record CarouselPresignedUrlsResult(Map<String, CarouselPresignedUpload> carouselPresignedUploads) {
		public CarouselPresignedUrlsResult {
			carouselPresignedUploads = Map.copyOf(carouselPresignedUploads);
		}
	}

	public record BannerPresignedUrlResult(String bannerPresignedUrl) {
	}
}
