package com.beat.apis.booking.application.dto;

public record GuestBookingRetrieveRequest(
	String bookerName,
	String birthDate,
	String bookerPhoneNumber,
	String password
) {
}
