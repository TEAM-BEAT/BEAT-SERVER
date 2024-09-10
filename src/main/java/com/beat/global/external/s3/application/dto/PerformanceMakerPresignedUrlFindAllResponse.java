package com.beat.global.external.s3.application.dto;

import java.util.Map;

public record PerformanceMakerPresignedUrlFindAllResponse(
        Map<String, Map<String, String>> performanceMakerPresignedUrls
) {
    public static PerformanceMakerPresignedUrlFindAllResponse from(Map<String, Map<String, String>> performanceMakerPresignedUrls) {
        return new PerformanceMakerPresignedUrlFindAllResponse(performanceMakerPresignedUrls);
    }
}