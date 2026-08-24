package com.beat.apps.api.booking.api.request


data class GuestBookingRetrieveRequest(
    val bookerName: String,
    val birthDate: String,
    val bookerPhoneNumber: String,
    val password: String,
)
