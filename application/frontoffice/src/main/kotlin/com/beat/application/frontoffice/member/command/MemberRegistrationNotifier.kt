package com.beat.application.frontoffice.member.command

fun interface MemberRegistrationNotifier {
    fun send(notification: MemberRegistrationNotification)
}
