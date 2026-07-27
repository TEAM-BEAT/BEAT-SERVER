package com.beat.admin.promotion.api.response;

import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult;

public record BannerPresignedUrlFindResponse(
	String bannerPresignedUrl,
	BannerPresignedUploadResponse bannerPresignedUpload
) {
	private static BannerPresignedUrlFindResponse of(String bannerPresignedUrl, String bannerImageKey) {
		return new BannerPresignedUrlFindResponse(bannerPresignedUrl,
			new BannerPresignedUploadResponse(bannerPresignedUrl, bannerImageKey));
	}

	public static BannerPresignedUrlFindResponse from(BannerPresignedUrlResult result) {
		return BannerPresignedUrlFindResponse.of(result.bannerPresignedUrl(), result.bannerImageKey());
	}

	public record BannerPresignedUploadResponse(String uploadUrl, String imageKey) {
	}
}
