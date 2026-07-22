package com.beat.apis.ticket.facade

import com.beat.apis.booking.api.type.BookingStatusType
import com.beat.apis.schedule.api.type.ScheduleNumberType
import com.beat.apis.ticket.api.request.TicketDeleteRequest
import com.beat.apis.ticket.api.request.TicketRefundRequest
import com.beat.apis.ticket.api.response.TicketRetrieveResponse
import com.beat.apis.ticket.api.request.TicketUpdateRequest
import com.beat.apis.ticket.application.command.TicketBookingIdsCommand
import com.beat.apis.ticket.application.command.TicketBookingStatus
import com.beat.apis.ticket.application.command.TicketCommandService
import com.beat.apis.ticket.application.command.TicketStatusUpdate
import com.beat.apis.ticket.application.command.TicketUpdateCommand
import com.beat.apis.ticket.application.query.TicketListQuery
import com.beat.apis.ticket.application.query.TicketQueryService
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
    ): TicketRetrieveResponse = TicketRetrieveResponse.from(
        ticketQueryService.findAllTicketsByConditions(
            memberId,
            performanceId,
            TicketListQuery(
                scheduleNumbers = scheduleNumbers.orEmpty().map { it.name },
                bookingStatuses = bookingStatuses.orEmpty().map { it.name },
            ),
        ),
    )

    fun searchTickets(
        memberId: Long,
        performanceId: Long,
        searchWord: String?,
        scheduleNumbers: List<ScheduleNumberType>?,
        bookingStatuses: List<BookingStatusType>?,
    ): TicketRetrieveResponse = TicketRetrieveResponse.from(
        ticketQueryService.searchAllTicketsByConditions(
            memberId,
            performanceId,
            TicketListQuery(
                searchWord = searchWord,
                scheduleNumbers = scheduleNumbers.orEmpty().map { it.name },
                bookingStatuses = bookingStatuses.orEmpty().map { it.name },
            ),
        ),
    )

    fun updateTickets(memberId: Long, request: TicketUpdateRequest) =
        ticketCommandService.updateTickets(
            memberId,
            TicketUpdateCommand(
                performanceId = requireNotNull(request.performanceId),
                bookingList = requireNotNull(request.bookingList).map { detail ->
                    TicketStatusUpdate(
                        bookingId = requireNotNull(detail.bookingId),
                        bookingStatus = TicketBookingStatus.valueOf(requireNotNull(detail.bookingStatus).name),
                    )
                },
            ),
        )

    fun refundTickets(memberId: Long, request: TicketRefundRequest) =
        ticketCommandService.refundTicketsByBookingIds(
            memberId,
            TicketBookingIdsCommand(
                performanceId = requireNotNull(request.performanceId),
                bookingIds = requireNotNull(request.bookingList).map { requireNotNull(it.bookingId) },
            ),
        )

    fun deleteTickets(memberId: Long, request: TicketDeleteRequest) =
        ticketCommandService.deleteTicketsByBookingIds(
            memberId,
            TicketBookingIdsCommand(
                performanceId = requireNotNull(request.performanceId),
                bookingIds = requireNotNull(request.bookingList).map { requireNotNull(it.bookingId) },
            ),
        )
}
