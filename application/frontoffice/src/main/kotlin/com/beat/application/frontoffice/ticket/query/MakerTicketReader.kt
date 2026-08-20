package com.beat.application.frontoffice.ticket.query

interface MakerTicketReader {
    fun findTickets(
        performanceId: Long,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel>

    fun searchTickets(
        performanceId: Long,
        searchWord: String,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel>

    fun findSchedules(performanceId: Long): List<MakerTicketScheduleReadModel>
}
