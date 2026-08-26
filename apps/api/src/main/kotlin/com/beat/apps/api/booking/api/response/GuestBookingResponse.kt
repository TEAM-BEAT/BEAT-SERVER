package com.beat.apps.api.booking.api.response

import com.beat.application.frontoffice.booking.booker.result.BookingCreationResult
import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.performance.api.type.BankNameType
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class GuestBookingResponse
private constructor(
    val bookingId: Long,
    val scheduleId: Long,
    val userId: Long,
    val purchaseTicketCount: Int,
    val scheduleNumber: ScheduleNumberType,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val bookingStatus: BookingStatusType,
    @field:Schema(types = ["string", "null"], requiredMode = Schema.RequiredMode.REQUIRED)
    val bankName: BankNameType?,
    @field:Schema(types = ["string", "null"], requiredMode = Schema.RequiredMode.REQUIRED)
    val accountNumber: String?,
    val totalPaymentAmount: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(result: BookingCreationResult): GuestBookingResponse =
            GuestBookingResponse(
                bookingId = result.bookingId,
                scheduleId = result.scheduleId,
                userId = result.userId,
                purchaseTicketCount = result.purchaseTicketCount,
                scheduleNumber = ScheduleNumberType.valueOf(result.scheduleNumber),
                bookerName = result.bookerName,
                bookerPhoneNumber = result.bookerPhoneNumber,
                bookingStatus = BookingStatusType.valueOf(result.bookingStatus),
                bankName = result.bankName?.let(BankNameType::valueOf),
                accountNumber = result.accountNumber,
                totalPaymentAmount = result.totalPaymentAmount,
                createdAt = result.createdAt,
            )
    }
}
