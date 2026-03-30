package com.beat.apis.schedule.application.dto.response;

public record TicketAvailabilityResponse(
	Long scheduleId,
	String scheduleNumber,
	int totalTicketCount,
	int soldTicketCount,
	int availableTicketCount,
	int requestedTicketCount,
	boolean isAvailable
) {
	public static TicketAvailabilityResponse of(Long scheduleId, String scheduleNumber, int totalTicketCount,
		int soldTicketCount, int availableTicketCount, int requestedTicketCount, boolean isAvailable) {
		return new TicketAvailabilityResponse(scheduleId, scheduleNumber, totalTicketCount, soldTicketCount,
			availableTicketCount, requestedTicketCount, isAvailable);
	}
}
