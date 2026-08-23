package com.beat.apis.booking.api.request


data class MemberBookingRequest(
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
)
