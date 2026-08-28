package com.beat.application.frontoffice.booking.booker.command.result

import java.time.LocalDateTime

data class BookingCreationResult(
    val bookingId: Long,
    val scheduleId: Long,
    val userId: Long,
    val purchaseTicketCount: Int,
    val scheduleNumber: String,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val bookingStatus: String,
    val bankName: String?,
    val accountNumber: String?,
    val totalPaymentAmount: Int,
    val createdAt: LocalDateTime,
)

data class GuestBookingCreationOutcome(
    val booking: BookingCreationResult,
    val sessionToken: String?,
)

data class GuestBookingAccessResult(val userId: Long)

data class BookingRefundResult(
    val bookingId: Long,
    val bookingStatus: String,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
)

data class BookingCancelResult(
    val bookingId: Long,
    val bookingStatus: String,
)
