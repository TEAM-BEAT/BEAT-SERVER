package com.beat.domain.schedule.application.dto.request;

public record TicketAvailabilityRequest(
	Integer purchaseTicketCount

) {
	public static TicketAvailabilityRequest of(Integer purchaseTicketCount) {
		return new TicketAvailabilityRequest(purchaseTicketCount);
	}
}
