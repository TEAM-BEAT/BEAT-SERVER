package com.beat.admin.application.dto.response;

import com.beat.contracts.storage.BannerPresignedUrl;

public record BannerPresignedUrlFindResponse(
	String bannerPresignedUrl
) {

	public static BannerPresignedUrlFindResponse from(BannerPresignedUrl response) {
		return new BannerPresignedUrlFindResponse(response.bannerPresignedUrl());
	}
}
