package com.beat.contracts.notification


fun interface MemberNotificationPort {

    fun send(notification: MemberNotification)
}
