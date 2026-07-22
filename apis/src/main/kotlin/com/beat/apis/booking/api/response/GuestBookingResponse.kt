package com.beat.apis.booking.api.response

import com.beat.apis.booking.api.type.BookingStatusType
import com.beat.apis.booking.application.result.BookingCreationResult
import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.schedule.api.type.ScheduleNumberType
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class GuestBookingResponse private constructor(
    val bookingId: Long?,
    val scheduleId: Long?,
    val userId: Long?,
    val purchaseTicketCount: Int,
    val scheduleNumber: ScheduleNumberType?,
    val bookerName: String?,
    val bookerPhoneNumber: String?,
    val bookingStatus: BookingStatusType?,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val totalPaymentAmount: Int,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(result: BookingCreationResult): GuestBookingResponse = GuestBookingResponse(
            bookingId = result.bookingId,
            scheduleId = result.scheduleId,
            userId = result.userId,
            purchaseTicketCount = result.purchaseTicketCount,
            scheduleNumber = result.scheduleNumber?.let(ScheduleNumberType::valueOf),
            bookerName = result.bookerName,
            bookerPhoneNumber = result.bookerPhoneNumber,
            bookingStatus = result.bookingStatus?.let(BookingStatusType::valueOf),
            bankName = result.bankName?.let(BankNameType::valueOf),
            accountNumber = result.accountNumber,
            totalPaymentAmount = result.totalPaymentAmount,
            createdAt = result.createdAt,
        )
    }
}
