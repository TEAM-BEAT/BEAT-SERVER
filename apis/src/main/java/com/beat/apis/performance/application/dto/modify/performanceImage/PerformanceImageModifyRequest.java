package com.beat.apis.performance.application.dto.modify.performanceImage;

import org.springframework.lang.Nullable;

public record PerformanceImageModifyRequest(
	@Nullable
	Long performanceImageId,
	String performanceImage
) {
}
