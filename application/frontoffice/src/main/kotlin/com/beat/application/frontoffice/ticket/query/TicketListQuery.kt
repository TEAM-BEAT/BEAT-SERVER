package com.beat.application.frontoffice.ticket.query

data class TicketListQuery(
    val searchWord: String? = null,
    val scheduleNumbers: List<String> = emptyList(),
    val bookingStatuses: List<String> = emptyList(),
)
