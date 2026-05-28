package com.beat.apis.home.application.dto;

import com.beat.global.support.jackson.CdnImageUrl;

public record HomePerformanceDetail(
	Long performanceId,
	String performanceTitle,
	String performancePeriod,
	int ticketPrice,
	int dueDate,
	String genre,
	@CdnImageUrl String posterImage,
	String performanceVenue
) {

	public static HomePerformanceDetail of(Long performanceId, String performanceTitle, String performancePeriod,
		int ticketPrice, int dueDate, String genre, String posterImage, String performanceVenue) {
		return new HomePerformanceDetail(
			performanceId,
			performanceTitle,
			performancePeriod,
			ticketPrice,
			dueDate,
			genre,
			posterImage,
			performanceVenue
		);
	}
}
