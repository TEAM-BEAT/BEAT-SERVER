package com.beat.domain.performance.application.dto.home;

import com.beat.domain.performance.domain.Performance;

public record HomePerformanceDetail(
	Long performanceId,
	String performanceTitle,
	String performancePeriod,
	int ticketPrice,
	int dueDate,
	String genre,
	String posterImage,
	String performanceVenue
) {

	public static HomePerformanceDetail of(Performance performance, int minDueDate) {
		return new HomePerformanceDetail(
			performance.getId(),
			performance.getPerformanceTitle(),
			performance.getPerformancePeriod(),
			performance.getTicketPrice(),
			minDueDate,
			performance.getGenre().name(),
			performance.getPosterImage(),
			performance.getPerformanceVenue()
		);
	}
}