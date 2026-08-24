package com.beat.apps.api.booking.api.request


data class GuestBookingRequest(
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val birthDate: String,
    val password: String,
)
