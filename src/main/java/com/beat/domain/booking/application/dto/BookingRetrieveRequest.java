package com.beat.domain.booking.application.dto;

public record BookingRetrieveRequest(
        String bookerName,
        String birthDate,
        String bookerPhoneNumber,
        String password
) {
    public static BookingRetrieveRequest of(String bookerName, String birthDate, String bookerPhoneNumber, String password) {
        return new BookingRetrieveRequest(bookerName, birthDate, bookerPhoneNumber, password);
    }
}