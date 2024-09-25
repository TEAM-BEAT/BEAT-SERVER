package com.beat.global.external.s3.application.dto;

public record BannerPresignedUrlFindResponse(
        String bannerPresignedUrl
) {
    public static BannerPresignedUrlFindResponse from(String bannerPresignedUrl) {
        return new BannerPresignedUrlFindResponse(bannerPresignedUrl);
    }
}