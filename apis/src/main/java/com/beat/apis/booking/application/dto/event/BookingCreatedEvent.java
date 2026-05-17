package com.beat.apis.booking.application.dto.event;

import java.time.LocalDateTime;

public record BookingCreatedEvent(
	LocalDateTime bookingDateTime,
	String performanceTitle,
	int purchaseTicketCount,
	String bookerName,
	String scheduleDisplayName,
	int currentSoldTicketCount,
	int totalTicketCount
) {
	public static BookingCreatedEvent of(LocalDateTime bookingDateTime, String performanceTitle, int purchaseTicketCount,
		String bookerName, String scheduleDisplayName, int currentSoldTicketCount, int totalTicketCount) {
		return new BookingCreatedEvent(
			bookingDateTime,
			performanceTitle,
			purchaseTicketCount,
			bookerName,
			scheduleDisplayName,
			currentSoldTicketCount,
			totalTicketCount
		);
	}
}
