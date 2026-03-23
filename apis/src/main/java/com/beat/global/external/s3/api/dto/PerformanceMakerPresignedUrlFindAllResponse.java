package com.beat.global.external.s3.api.dto;

import com.beat.contracts.storage.PerformancePresignedUrls;
import java.util.Map;

public record PerformanceMakerPresignedUrlFindAllResponse(
	Map<String, Map<String, String>> performanceMakerPresignedUrls
) {

	public static PerformanceMakerPresignedUrlFindAllResponse from(PerformancePresignedUrls response) {
		return new PerformanceMakerPresignedUrlFindAllResponse(response.performanceMakerPresignedUrls());
	}
}
