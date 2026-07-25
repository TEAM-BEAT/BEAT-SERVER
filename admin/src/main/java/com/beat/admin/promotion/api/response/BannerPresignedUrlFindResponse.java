package com.beat.admin.promotion.api.response;

import com.beat.admin.promotion.application.result.AdminPromotionPresignedUrlResults.BannerPresignedUrlResult;

public record BannerPresignedUrlFindResponse(
	String bannerPresignedUrl
) {
	private static BannerPresignedUrlFindResponse of(String bannerPresignedUrl) {
		return new BannerPresignedUrlFindResponse(bannerPresignedUrl);
	}

	public static BannerPresignedUrlFindResponse from(BannerPresignedUrlResult result) {
		return BannerPresignedUrlFindResponse.of(result.bannerPresignedUrl());
	}
}
