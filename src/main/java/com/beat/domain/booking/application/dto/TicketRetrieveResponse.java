package com.beat.domain.booking.application.dto;

import java.util.List;

public record TicketRetrieveResponse(
	String performanceTitle,
	int totalScheduleCount,
	List<TicketDetail> bookingList
) {
	public static TicketRetrieveResponse of(
		String performanceTitle,
		int totalScheduleCount,
		List<TicketDetail> bookingList) {
		return new TicketRetrieveResponse(performanceTitle, totalScheduleCount, bookingList);
	}
}
