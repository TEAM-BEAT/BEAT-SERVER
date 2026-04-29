package com.beat.apis.booking.application.dto.event;

import java.time.LocalDateTime;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.schedule.domain.Schedule;

public record BookingCreatedEvent(
	LocalDateTime bookingDateTime,
	String performanceTitle,
	int purchaseTicketCount,
	String bookerName,
	String scheduleDisplayName,
	int currentSoldTicketCount,
	int totalTicketCount
) {
	public static BookingCreatedEvent of(Booking booking, Schedule schedule, String performanceTitle) {
		return new BookingCreatedEvent(
			booking.getCreatedAt(),
			performanceTitle,
			booking.getPurchaseTicketCount(),
			booking.getBookerName(),
			schedule.getScheduleNumber().getDisplayName(),
			schedule.getSoldTicketCount(),
			schedule.getTotalTicketCount()
		);
	}
}
