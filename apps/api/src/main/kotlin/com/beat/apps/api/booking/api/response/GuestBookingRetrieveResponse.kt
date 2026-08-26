package com.beat.apps.api.booking.api.response

import com.beat.application.frontoffice.booking.booker.result.BookingRetrieveResult
import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.performance.api.type.BankNameType
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class GuestBookingRetrieveResponse
private constructor(
    val bookingId: Long,
    val scheduleId: Long,
    val performanceId: Long,
    val performanceTitle: String,
    val performanceDate: LocalDateTime,
    val performanceVenue: String,
    val purchaseTicketCount: Int,
    val scheduleNumber: ScheduleNumberType,
    val bookerName: String,
    val performanceContact: String,
    @field:Schema(types = ["string", "null"], requiredMode = Schema.RequiredMode.REQUIRED)
    val bankName: BankNameType?,
    @field:Schema(types = ["string", "null"], requiredMode = Schema.RequiredMode.REQUIRED)
    val accountNumber: String?,
    @field:Schema(types = ["string", "null"], requiredMode = Schema.RequiredMode.REQUIRED)
    val accountHolder: String?,
    val dueDate: Int,
    val bookingStatus: BookingStatusType,
    val createdAt: LocalDateTime,
    @field:CdnImageUrl val posterImage: String,
    val totalPaymentAmount: Int,
) {
    companion object {
        fun from(result: BookingRetrieveResult): GuestBookingRetrieveResponse =
            GuestBookingRetrieveResponse(
                bookingId = result.bookingId,
                scheduleId = result.scheduleId,
                performanceId = result.performanceId,
                performanceTitle = result.performanceTitle,
                performanceDate = result.performanceDate,
                performanceVenue = result.performanceVenue,
                purchaseTicketCount = result.purchaseTicketCount,
                scheduleNumber = ScheduleNumberType.valueOf(result.scheduleNumber),
                bookerName = result.bookerName,
                performanceContact = result.performanceContact,
                bankName = result.bankName?.let(BankNameType::valueOf),
                accountNumber = result.accountNumber,
                accountHolder = result.accountHolder,
                dueDate = result.dueDate,
                bookingStatus = BookingStatusType.valueOf(result.bookingStatus),
                createdAt = result.createdAt,
                posterImage = result.posterImage,
                totalPaymentAmount = result.totalPaymentAmount,
            )
    }
}
