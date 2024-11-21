package com.beat.domain.performance.application.dto.modify.performanceImage;

import javax.annotation.Nullable;

public record PerformanceImageModifyRequest(
	@Nullable
	Long performanceImageId,
	String performanceImage
) {
}
