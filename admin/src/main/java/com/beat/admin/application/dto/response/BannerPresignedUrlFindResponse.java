package com.beat.admin.application.dto.response;

import com.beat.contracts.storage.BannerPresignedUrl;

public record BannerPresignedUrlFindResponse(
	String bannerPresignedUrl
) {
	private static BannerPresignedUrlFindResponse of(String bannerPresignedUrl) {
		return new BannerPresignedUrlFindResponse(bannerPresignedUrl);
	}

	public static BannerPresignedUrlFindResponse from(BannerPresignedUrl response) {
		return BannerPresignedUrlFindResponse.of(response.bannerPresignedUrl());
	}
}
