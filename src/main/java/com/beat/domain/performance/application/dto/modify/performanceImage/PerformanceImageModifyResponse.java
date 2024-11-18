package com.beat.domain.performance.application.dto.modify.performanceImage;

public record PerformanceImageModifyResponse(
	Long performanceImageId,
	String performanceImage
) {

	public static PerformanceImageModifyResponse of(Long performanceImageId, String performanceImage) {
		return new PerformanceImageModifyResponse(performanceImageId, performanceImage);
	}
}
