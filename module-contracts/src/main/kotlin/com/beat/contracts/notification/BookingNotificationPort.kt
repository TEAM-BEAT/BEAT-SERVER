package com.beat.contracts.notification


fun interface BookingNotificationPort {

    fun send(notification: BookingNotification)
}
