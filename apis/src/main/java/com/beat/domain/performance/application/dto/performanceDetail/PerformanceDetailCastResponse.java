package com.beat.domain.performance.application.dto.performanceDetail;

public record PerformanceDetailCastResponse(
	Long castId,
	String castName,
	String castRole,
	String castPhoto
) {
	public static PerformanceDetailCastResponse of(Long castId, String castName, String castRole, String castPhoto) {
		return new PerformanceDetailCastResponse(castId, castName, castRole, castPhoto);
	}
}
