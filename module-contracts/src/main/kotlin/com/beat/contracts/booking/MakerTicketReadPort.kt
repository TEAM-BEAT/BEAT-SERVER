package com.beat.contracts.booking

import com.beat.contracts.booking.readmodel.MakerTicketBookingStatus
import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel
import com.beat.contracts.booking.readmodel.MakerTicketScheduleNumber

interface MakerTicketReadPort {

    fun findTickets(
        performanceId: Long?,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel>

    fun searchTickets(
        performanceId: Long?,
        searchWord: String?,
        scheduleNumbers: List<MakerTicketScheduleNumber>,
        bookingStatuses: List<MakerTicketBookingStatus>,
    ): List<MakerTicketListItemReadModel>
}
