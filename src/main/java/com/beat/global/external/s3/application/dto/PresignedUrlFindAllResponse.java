package com.beat.global.external.s3.application.dto;

import java.util.Map;

public record PresignedUrlFindAllResponse(
        Map<String, Map<String, String>> presignedUrls
) {
    public static PresignedUrlFindAllResponse from(Map<String, Map<String, String>> presignedUrls) {
        return new PresignedUrlFindAllResponse(presignedUrls);
    }
}