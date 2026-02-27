package com.beat.domain.booking.application.dto.event;

import java.time.LocalDateTime;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.schedule.domain.Schedule;

public record BookingCreatedEvent(
	LocalDateTime bookingDateTime,
	String performanceTitle,
	int purchaseTicketCount,
	String bookerName,
	int currentSoldTicketCount,
	int totalTicketCount
) {
	public static BookingCreatedEvent of(Booking booking, Schedule schedule) {
		return new BookingCreatedEvent(
			booking.getCreatedAt(),
			schedule.getPerformance().getPerformanceTitle(),
			booking.getPurchaseTicketCount(),
			booking.getBookerName(),
			schedule.getSoldTicketCount(),
			schedule.getTotalTicketCount()
		);
	}
}
