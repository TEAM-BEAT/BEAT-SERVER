package com.beat.apis.performance.application.dto.modify.performanceImage;

import com.beat.global.support.jackson.CdnImageUrl;

public record PerformanceImageModifyResponse(
	Long performanceImageId,
	@CdnImageUrl String performanceImage
) {

	public static PerformanceImageModifyResponse of(Long performanceImageId, String performanceImage) {
		return new PerformanceImageModifyResponse(performanceImageId, performanceImage);
	}
}
