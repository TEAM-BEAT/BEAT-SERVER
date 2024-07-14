package com.beat.domain.booking.application.dto;

public record GuestBookingRetrieveRequest(
        String bookerName,
        String birthDate,
        String bookerPhoneNumber,
        String password
) {
//    public static GuestBookingRetrieveRequest of(String bookerName, String birthDate, String bookerPhoneNumber, String password) {
//        return new GuestBookingRetrieveRequest(bookerName, birthDate, bookerPhoneNumber, password);
//    }
}