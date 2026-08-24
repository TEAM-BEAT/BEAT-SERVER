package com.beat.application.frontoffice.ticket.maker.query

import com.beat.application.frontoffice.query.PresentationReadModel

@PresentationReadModel
data class MakerTicketScheduleReadModel(
    val scheduleId: Long,
    val totalTicketCount: Int,
    val soldTicketCount: Int,
    val scheduleNumber: String,
)
