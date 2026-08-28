package com.beat.application.frontoffice.booking.booker.command.event

fun interface BookingNotificationSender {
    fun send(event: BookingCreatedEvent)
}
