package com.beat.apps.api.ticket.facade

import com.beat.application.frontoffice.ticket.maker.command.TicketBookingIdsCommand
import com.beat.application.frontoffice.ticket.maker.command.TicketBookingStatus
import com.beat.application.frontoffice.ticket.maker.command.TicketCommandService
import com.beat.application.frontoffice.ticket.maker.command.TicketStatusUpdate
import com.beat.application.frontoffice.ticket.maker.command.TicketUpdateCommand
import com.beat.application.frontoffice.ticket.maker.query.TicketListQuery
import com.beat.application.frontoffice.ticket.maker.query.TicketQueryService
import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import com.beat.apps.api.ticket.api.request.TicketDeleteRequest
import com.beat.apps.api.ticket.api.request.TicketRefundRequest
import com.beat.apps.api.ticket.api.request.TicketUpdateRequest
import com.beat.apps.api.ticket.api.response.TicketRetrieveResponse
import org.springframework.stereotype.Service

@Service
class TicketFacade(
    private val ticketQueryService: TicketQueryService,
    private val ticketCommandService: TicketCommandService,
) {
    fun findTickets(
        memberId: Long,
        performanceId: Long,
        scheduleNumbers: List<ScheduleNumberType>?,
        bookingStatuses: List<BookingStatusType>?,
    ): TicketRetrieveResponse =
        TicketRetrieveResponse.from(
            ticketQueryService.findAllTicketsByConditions(
                memberId,
                performanceId,
                TicketListQuery(
                    scheduleNumbers = scheduleNumbers.orEmpty().map { it.name },
                    bookingStatuses = bookingStatuses.orEmpty().map { it.name },
                ),
            )
        )

    fun searchTickets(
        memberId: Long,
        performanceId: Long,
        searchWord: String?,
        scheduleNumbers: List<ScheduleNumberType>?,
        bookingStatuses: List<BookingStatusType>?,
    ): TicketRetrieveResponse =
        TicketRetrieveResponse.from(
            ticketQueryService.searchAllTicketsByConditions(
                memberId,
                performanceId,
                TicketListQuery(
                    searchWord = searchWord,
                    scheduleNumbers = scheduleNumbers.orEmpty().map { it.name },
                    bookingStatuses = bookingStatuses.orEmpty().map { it.name },
                ),
            )
        )

    fun updateTickets(memberId: Long, request: TicketUpdateRequest) =
        ticketCommandService.updateTickets(
            memberId,
            TicketUpdateCommand(
                performanceId = request.performanceId,
                bookingList =
                    request.bookingList.map { detail ->
                        TicketStatusUpdate(
                            bookingId = detail.bookingId,
                            bookingStatus = TicketBookingStatus.valueOf(detail.bookingStatus.name),
                        )
                    },
            ),
        )

    fun refundTickets(memberId: Long, request: TicketRefundRequest) =
        ticketCommandService.refundTicketsByBookingIds(
            memberId,
            TicketBookingIdsCommand(
                performanceId = request.performanceId,
                bookingIds = request.bookingList.map { it.bookingId },
            ),
        )

    fun deleteTickets(memberId: Long, request: TicketDeleteRequest) =
        ticketCommandService.deleteTicketsByBookingIds(
            memberId,
            TicketBookingIdsCommand(
                performanceId = request.performanceId,
                bookingIds = request.bookingList.map { it.bookingId },
            ),
        )
}
