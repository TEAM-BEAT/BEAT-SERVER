package com.beat.contracts.notification;

import java.time.LocalDateTime;

public record BookingNotification(
	LocalDateTime bookingDateTime,
	String performanceTitle,
	int purchaseTicketCount,
	String bookerName,
	String scheduleDisplayName,
	int currentSoldTicketCount,
	int totalTicketCount
) {
}
