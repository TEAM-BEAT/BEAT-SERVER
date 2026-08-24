package com.beat.apps.api.ticket.api.response

import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.application.frontoffice.ticket.maker.query.TicketDetailResult
import java.time.LocalDateTime

@ConsistentCopyVisibility
data class TicketDetail private constructor(
    val bookingId: Long?,
    val bookerName: String?,
    val bookerPhoneNumber: String?,
    val scheduleId: Long?,
    val purchaseTicketCount: Int,
    val createdAt: LocalDateTime?,
    val bookingStatus: BookingStatusType?,
    val scheduleNumber: String?,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val deletable: Boolean,
) {
    companion object {
        fun from(result: TicketDetailResult): TicketDetail = TicketDetail(
            bookingId = result.bookingId,
            bookerName = result.bookerName,
            bookerPhoneNumber = result.bookerPhoneNumber,
            scheduleId = result.scheduleId,
            purchaseTicketCount = result.purchaseTicketCount,
            createdAt = result.createdAt,
            bookingStatus = BookingStatusType.valueOf(result.bookingStatus),
            scheduleNumber = result.scheduleNumber,
            bankName = result.bankName,
            accountNumber = result.accountNumber,
            accountHolder = result.accountHolder,
            deletable = result.deletable,
        )
    }
}
