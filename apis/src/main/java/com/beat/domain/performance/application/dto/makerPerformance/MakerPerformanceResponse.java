package com.beat.domain.performance.application.dto.makerPerformance;

import java.util.List;

public record MakerPerformanceResponse(
	Long userId,
	List<MakerPerformanceDetailResponse> performances
) {
	public static MakerPerformanceResponse of(
		Long userId,
		List<MakerPerformanceDetailResponse> performances) {
		return new MakerPerformanceResponse(userId, performances);
	}
}
