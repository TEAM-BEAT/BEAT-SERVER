package com.beat.apis.performance.application.dto.performanceDetail;

import com.beat.global.support.jackson.CdnImageUrl;

public record PerformanceDetailCastResponse(
	Long castId,
	String castName,
	String castRole,
	@CdnImageUrl String castPhoto
) {
	public static PerformanceDetailCastResponse of(Long castId, String castName, String castRole, String castPhoto) {
		return new PerformanceDetailCastResponse(castId, castName, castRole, castPhoto);
	}
}
