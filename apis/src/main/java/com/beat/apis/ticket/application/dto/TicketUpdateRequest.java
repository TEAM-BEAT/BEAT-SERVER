package com.beat.apis.ticket.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TicketUpdateRequest(
	Long performanceId,
	String performanceTitle,
	int totalScheduleCount,
	List<TicketUpdateDetail> bookingList
) {
	public static TicketUpdateRequest of(
		Long performanceId,
		String performanceTitle,
		int totalScheduleCount,
		List<TicketUpdateDetail> bookingList) {
		return new TicketUpdateRequest(performanceId, performanceTitle, totalScheduleCount, bookingList);
	}
}
