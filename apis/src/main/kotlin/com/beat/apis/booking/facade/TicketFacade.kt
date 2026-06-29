package com.beat.apis.booking.facade

import com.beat.apis.booking.application.dto.TicketDeleteRequest
import com.beat.apis.booking.application.dto.TicketRefundRequest
import com.beat.apis.booking.application.dto.TicketRetrieveResponse
import com.beat.apis.booking.application.dto.TicketUpdateRequest
import com.beat.apis.booking.application.service.command.TicketCommandService
import com.beat.apis.booking.application.service.query.TicketQueryService
import com.beat.domain.booking.domain.BookingStatus
import com.beat.domain.schedule.domain.ScheduleNumber
import org.springframework.stereotype.Component

@Component
class TicketFacade(
    private val ticketQueryService: TicketQueryService,
    private val ticketCommandService: TicketCommandService,
) {

    fun findAllTicketsByConditions(
        memberId: Long,
        performanceId: Long,
        scheduleNumbers: List<ScheduleNumber>?,
        bookingStatuses: List<BookingStatus>?,
    ): TicketRetrieveResponse {
        return ticketQueryService.findAllTicketsByConditions(
            memberId = memberId,
            performanceId = performanceId,
            scheduleNumbers = scheduleNumbers,
            bookingStatuses = bookingStatuses,
        )
    }

    fun searchAllTicketsByConditions(
        memberId: Long,
        performanceId: Long,
        searchWord: String,
        scheduleNumbers: List<ScheduleNumber>?,
        bookingStatuses: List<BookingStatus>?,
    ): TicketRetrieveResponse {
        return ticketQueryService.searchAllTicketsByConditions(
            memberId = memberId,
            performanceId = performanceId,
            searchWord = searchWord,
            scheduleNumbers = scheduleNumbers,
            bookingStatuses = bookingStatuses,
        )
    }

    fun updateTickets(memberId: Long, request: TicketUpdateRequest) {
        ticketCommandService.updateTickets(memberId, request)
    }

    fun refundTicketsByBookingIds(memberId: Long, request: TicketRefundRequest) {
        ticketCommandService.refundTicketsByBookingIds(memberId, request)
    }

    fun deleteTicketsByBookingIds(memberId: Long, request: TicketDeleteRequest) {
        ticketCommandService.deleteTicketsByBookingIds(memberId, request)
    }
}