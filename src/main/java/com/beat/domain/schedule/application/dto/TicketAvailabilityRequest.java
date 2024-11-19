package com.beat.domain.schedule.application.dto;

public record TicketAvailabilityRequest(
	Integer purchaseTicketCount

) {
	public static TicketAvailabilityRequest of(Integer purchaseTicketCount) {
		return new TicketAvailabilityRequest(purchaseTicketCount);
	}
}