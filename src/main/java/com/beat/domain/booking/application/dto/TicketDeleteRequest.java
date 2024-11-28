package com.beat.domain.booking.application.dto;

import java.util.List;

public record TicketDeleteRequest(
	Long performanceId,
	List<Booking> bookingList
) {
	public static TicketDeleteRequest of(Long performanceId, List<Booking> bookingList) {
		return new TicketDeleteRequest(performanceId, bookingList);
	}

	public static record Booking(
		long bookingId
	) {
	}
}
