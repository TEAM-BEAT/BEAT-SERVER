package com.beat.application.frontoffice.ticket.query

import java.time.LocalDateTime

data class TicketRetrieveResult(
    val performanceTitle: String,
    val performanceTeamName: String,
    val totalScheduleCount: Int,
    val totalPerformanceTicketCount: Int,
    val totalPerformanceSoldTicketCount: Int,
    val bookingList: List<TicketDetailResult>,
)

data class TicketDetailResult(
    val bookingId: Long,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val createdAt: LocalDateTime,
    val bookingStatus: String,
    val scheduleNumber: String,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val deletable: Boolean,
)
