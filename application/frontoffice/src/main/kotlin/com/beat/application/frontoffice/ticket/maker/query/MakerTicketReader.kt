package com.beat.application.frontoffice.ticket.maker.query

import com.beat.application.frontoffice.query.PresentationReadModel

@PresentationReadModel
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
