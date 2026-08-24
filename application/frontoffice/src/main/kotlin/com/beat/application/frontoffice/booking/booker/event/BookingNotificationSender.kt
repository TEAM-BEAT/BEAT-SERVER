package com.beat.application.frontoffice.booking.booker.event

fun interface BookingNotificationSender {
    fun send(event: BookingCreatedEvent)
}
