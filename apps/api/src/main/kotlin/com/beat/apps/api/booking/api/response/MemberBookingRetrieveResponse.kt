package com.beat.apps.api.booking.api.response

import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.application.frontoffice.booking.booker.result.BookingRetrieveResult
import com.beat.apps.api.performance.api.type.BankNameType
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import com.beat.apps.api.web.jackson.CdnImageUrl
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class MemberBookingRetrieveResponse private constructor(
    val userId: Long?,
    val bookingId: Long?,
    val scheduleId: Long?,
    val performanceId: Long?,
    val performanceTitle: String?,
    val performanceDate: LocalDateTime?,
    val performanceVenue: String?,
    val purchaseTicketCount: Int,
    val scheduleNumber: ScheduleNumberType?,
    val bookerName: String?,
    val performanceContact: String?,
    val bankName: BankNameType?,
    val accountNumber: String?,
    val accountHolder: String?,
    val dueDate: Int,
    val bookingStatus: BookingStatusType?,
    val createdAt: LocalDateTime?,
    @field:CdnImageUrl val posterImage: String?,
    val totalPaymentAmount: Int,
) {
    companion object {
        fun from(result: BookingRetrieveResult): MemberBookingRetrieveResponse = MemberBookingRetrieveResponse(
            userId = result.userId,
            bookingId = result.bookingId,
            scheduleId = result.scheduleId,
            performanceId = result.performanceId,
            performanceTitle = result.performanceTitle,
            performanceDate = result.performanceDate,
            performanceVenue = result.performanceVenue,
            purchaseTicketCount = result.purchaseTicketCount,
            scheduleNumber = result.scheduleNumber?.let(ScheduleNumberType::valueOf),
            bookerName = result.bookerName,
            performanceContact = result.performanceContact,
            bankName = result.bankName?.let(BankNameType::valueOf),
            accountNumber = result.accountNumber,
            accountHolder = result.accountHolder,
            dueDate = result.dueDate,
            bookingStatus = result.bookingStatus?.let(BookingStatusType::valueOf),
            createdAt = result.createdAt,
            posterImage = result.posterImage,
            totalPaymentAmount = result.totalPaymentAmount,
        )
    }
}
