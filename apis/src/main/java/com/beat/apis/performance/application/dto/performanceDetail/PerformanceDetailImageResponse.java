package com.beat.apis.performance.application.dto.performanceDetail;

import com.beat.global.support.jackson.CdnImageUrl;

public record PerformanceDetailImageResponse(
	Long performanceImageId,
	@CdnImageUrl String performanceImage
) {
	public static PerformanceDetailImageResponse of(Long performanceImageId, String performanceImage) {
		return new PerformanceDetailImageResponse(performanceImageId, performanceImage);
	}
}
