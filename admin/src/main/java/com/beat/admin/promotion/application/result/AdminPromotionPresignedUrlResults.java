package com.beat.admin.promotion.application.result;

import java.util.Map;

public final class AdminPromotionPresignedUrlResults {

	private AdminPromotionPresignedUrlResults() {
	}

	public record CarouselPresignedUrlsResult(Map<String, String> carouselPresignedUrls) {
		public CarouselPresignedUrlsResult {
			carouselPresignedUrls = Map.copyOf(carouselPresignedUrls);
		}
	}

	public record BannerPresignedUrlResult(String bannerPresignedUrl) {
	}
}
