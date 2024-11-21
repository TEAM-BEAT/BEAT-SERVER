package com.beat.domain.performance.application.dto.makerPerformance;

public record MakerPerformanceDetailResponse(
	Long performanceId,
	String genre,
	String performanceTitle,
	String posterImage,
	String performancePeriod,
	int minDueDate
) {
	public static MakerPerformanceDetailResponse of(
		Long performanceId,
		String genre,
		String performanceTitle,
		String posterImage,
		String performancePeriod,
		int minDueDate) {  // minDueDate 매개변수 추가
		return new MakerPerformanceDetailResponse(performanceId, genre, performanceTitle, posterImage,
			performancePeriod, minDueDate);
	}
}
