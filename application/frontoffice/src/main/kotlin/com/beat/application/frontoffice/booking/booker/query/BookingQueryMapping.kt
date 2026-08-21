package com.beat.application.frontoffice.booking.booker.query

import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.calculatePaymentAmountForRead
import com.beat.application.frontoffice.booking.booker.result.BookingRetrieveResult
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.performance.vo.TicketPrice
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun BookerBookingReadModel.toResult(today: LocalDate): BookingRetrieveResult {
    val schedule = schedule
        ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
    val performance = performance
        ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.PERFORMANCE_NOT_FOUND)
    val amount = totalPaymentAmount
        ?: calculatePaymentAmountForRead(TicketPrice.of(performance.ticketPrice), purchaseTicketCount)
    return BookingRetrieveResult(
        userId = userId,
        bookingId = bookingId,
        scheduleId = schedule.scheduleId,
        performanceId = performance.performanceId,
        performanceTitle = performance.performanceTitle,
        performanceDate = schedule.performanceDate,
        performanceVenue = performance.performanceVenue,
        purchaseTicketCount = purchaseTicketCount,
        scheduleNumber = schedule.scheduleNumber,
        bookerName = bookerName,
        performanceContact = performance.performanceContact,
        bankName = performance.bankName,
        accountNumber = performance.accountNumber,
        accountHolder = performance.accountHolder,
        dueDate = ChronoUnit.DAYS.between(today, schedule.performanceDate.toLocalDate()).toInt(),
        bookingStatus = bookingStatus,
        createdAt = createdAt,
        posterImage = performance.posterImage,
        totalPaymentAmount = amount,
    )
}
