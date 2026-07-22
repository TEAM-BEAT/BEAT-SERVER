package com.beat.apis.booking.application.command

data class MemberBookingCommand(
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
)

data class GuestBookingCommand(
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val birthDate: String,
    val password: String,
)

data class GuestBookingAuthenticationCommand(
    val bookerName: String,
    val birthDate: String,
    val bookerPhoneNumber: String,
    val password: String,
)

data class BookingRefundCommand(
    val bookingId: Long,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
)

data class BookingCancelCommand(
    val bookingId: Long,
)
