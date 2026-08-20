package com.beat.application.frontoffice.ticket.command

data class TicketPaymentConfirmedEvent(
    val bookingId: Long,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val performanceTitle: String,
) {
    override fun toString(): String = "TicketPaymentConfirmedEvent[bookingId=$bookingId]"
}
