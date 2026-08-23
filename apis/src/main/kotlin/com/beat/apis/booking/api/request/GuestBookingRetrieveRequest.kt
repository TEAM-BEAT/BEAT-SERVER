package com.beat.apis.booking.api.request


data class GuestBookingRetrieveRequest(
    val bookerName: String,
    val birthDate: String,
    val bookerPhoneNumber: String,
    val password: String,
)
