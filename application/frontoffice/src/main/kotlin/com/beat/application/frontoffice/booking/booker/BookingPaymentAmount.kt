package com.beat.application.frontoffice.booking.booker

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.performance.vo.TicketPrice

internal fun calculatePaymentAmountForCommand(ticketPrice: TicketPrice, quantity: Int): Int =
    calculatePaymentAmount(ticketPrice, quantity, BookingApplicationErrorCode.TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE)

internal fun calculatePaymentAmountForRead(ticketPrice: TicketPrice, quantity: Int): Int =
    calculatePaymentAmount(ticketPrice, quantity, BookingApplicationErrorCode.STORED_TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE)

private fun calculatePaymentAmount(
    ticketPrice: TicketPrice,
    quantity: Int,
    outOfRangeError: BookingApplicationErrorCode,
): Int {
    val amount = ticketPrice.totalFor(quantity)
    if (amount > Int.MAX_VALUE) {
        throw FrontofficeApplicationException(outOfRangeError)
    }
    return amount.toInt()
}
