package com.beat.domain.performance.application.dto.performanceDetail;

public record PerformanceDetailImageResponse(
	Long performanceImageId,
	String performanceImage
) {
	public static PerformanceDetailImageResponse of(Long performanceImageId, String performanceImage) {
		return new PerformanceDetailImageResponse(performanceImageId, performanceImage);
	}
}
