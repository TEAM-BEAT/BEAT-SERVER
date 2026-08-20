package com.beat.application.frontoffice.ticket.query

data class MakerTicketScheduleReadModel(
    val scheduleId: Long,
    val totalTicketCount: Int,
    val soldTicketCount: Int,
    val scheduleNumber: String,
)
