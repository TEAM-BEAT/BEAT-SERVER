package com.beat.domain.booking.application.dto;

import java.util.List;

public record TicketRetrieveResponse(
	String performanceTitle,
	String performanceTeamName,
	int totalScheduleCount,
	int totalPerformanceTicketCount,
	int totalPerformanceSoldTicketCount,
	List<TicketDetail> bookingList
) {
	public static TicketRetrieveResponse of(
		String performanceTitle,
		String performanceTeamName,
		int totalScheduleCount,
		int totalPerformanceTicketCount,
		int totalPerformanceSoldTicketCount,
		List<TicketDetail> bookingList) {
		return new TicketRetrieveResponse(performanceTitle, performanceTeamName, totalScheduleCount,
			totalPerformanceTicketCount, totalPerformanceSoldTicketCount, bookingList);
	}
}
