package com.beat.admin.promotion.api.response;

import java.util.Map;

import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.CarouselPresignedUrlsResult;
import com.beat.contracts.storage.CarouselPresignedUpload;

public record CarouselPresignedUrlFindAllResponse(
	Map<String, String> carouselPresignedUrls,
	Map<String, CarouselPresignedUploadResponse> carouselPresignedUploads
) {
	private static CarouselPresignedUrlFindAllResponse of(
		Map<String, CarouselPresignedUpload> carouselPresignedUploads) {
		Map<String, String> carouselPresignedUrls = carouselPresignedUploads.entrySet().stream()
			.collect(java.util.stream.Collectors.toUnmodifiableMap(
				Map.Entry::getKey,
				entry -> entry.getValue().getUploadUrl()
			));
		Map<String, CarouselPresignedUploadResponse> uploadResponses = carouselPresignedUploads.entrySet().stream()
			.collect(java.util.stream.Collectors.toUnmodifiableMap(
				Map.Entry::getKey,
				entry -> CarouselPresignedUploadResponse.from(entry.getValue())
			));
		return new CarouselPresignedUrlFindAllResponse(carouselPresignedUrls, uploadResponses);
	}

	public static CarouselPresignedUrlFindAllResponse from(CarouselPresignedUrlsResult result) {
		return CarouselPresignedUrlFindAllResponse.of(result.carouselPresignedUploads());
	}

	public record CarouselPresignedUploadResponse(String uploadUrl, String imageKey) {
		private static CarouselPresignedUploadResponse from(CarouselPresignedUpload upload) {
			return new CarouselPresignedUploadResponse(upload.getUploadUrl(), upload.getImageKey());
		}
	}
}
