package com.beat.apis.performance.application.dto.create;

import com.beat.global.support.jackson.CdnImageUrl;

public record PerformanceImageResponse(
	Long imageId,
	@CdnImageUrl String imageUrl
) {
	public static PerformanceImageResponse of(
		Long imageId,
		String imageUrl
	) {
		return new PerformanceImageResponse(
			imageId,
			imageUrl
		);
	}
}
