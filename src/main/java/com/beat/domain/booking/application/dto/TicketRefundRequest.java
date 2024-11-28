package com.beat.domain.booking.application.dto;

import java.util.List;

public record TicketRefundRequest(
	Long performanceId,
	List<Booking> bookingList
) {
	public static TicketRefundRequest of(Long performanceId, List<Booking> bookingList) {
		return new TicketRefundRequest(performanceId, bookingList);
	}

	public static record Booking(
		long bookingId
	) {
	}
}
