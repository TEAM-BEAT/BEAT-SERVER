package com.beat.domain.booking.application.dto;

import java.util.List;

public record TicketCancelRequest(
	Long performanceId,
	List<Long> bookingList
) {
	public static TicketCancelRequest of(Long performanceId, List<Long> bookingList) {
		return new TicketCancelRequest(performanceId, bookingList);
	}
}
