package com.beat.apis.ticket.facade

import com.beat.apis.booking.api.type.BookingStatusType
import com.beat.apis.schedule.api.type.ScheduleNumberType
import com.beat.application.frontoffice.ticket.maker.query.TicketListQuery
import com.beat.application.frontoffice.ticket.maker.query.TicketQueryService
import com.beat.application.frontoffice.ticket.maker.query.TicketRetrieveResult
import com.beat.application.frontoffice.ticket.maker.command.TicketCommandService
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TicketFacadeTest {

    private val ticketQueryService = Mockito.mock(TicketQueryService::class.java)
    private val ticketCommandService = Mockito.mock(TicketCommandService::class.java)
    private val ticketFacade = TicketFacade(ticketQueryService, ticketCommandService)

    @Test
    fun `find tickets delegates with api filters mapped to application query`() {
        val memberId = 1L
        val performanceId = 100L
        val scheduleNumbers = listOf(ScheduleNumberType.FIRST)
        val bookingStatuses = listOf(BookingStatusType.CHECKING_PAYMENT)
        val query = TicketListQuery(null, listOf("FIRST"), listOf("CHECKING_PAYMENT"))
        val expected = TicketRetrieveResult("title", "team", 1, 100, 10, emptyList())
        Mockito.`when`(ticketQueryService.findAllTicketsByConditions(memberId, performanceId, query))
            .thenReturn(expected)

        ticketFacade.findTickets(memberId, performanceId, scheduleNumbers, bookingStatuses)

        Mockito.verify(ticketQueryService).findAllTicketsByConditions(memberId, performanceId, query)
    }

    @Test
    fun `search tickets delegates with api filters mapped to application query`() {
        val memberId = 1L
        val performanceId = 100L
        val searchWord = "홍길동"
        val scheduleNumbers = listOf(ScheduleNumberType.FIRST)
        val bookingStatuses = listOf(BookingStatusType.CHECKING_PAYMENT)
        val query = TicketListQuery(searchWord, listOf("FIRST"), listOf("CHECKING_PAYMENT"))
        val expected = TicketRetrieveResult("title", "team", 1, 100, 10, emptyList())
        Mockito.`when`(ticketQueryService.searchAllTicketsByConditions(memberId, performanceId, query))
            .thenReturn(expected)

        ticketFacade.searchTickets(memberId, performanceId, searchWord, scheduleNumbers, bookingStatuses)

        Mockito.verify(ticketQueryService).searchAllTicketsByConditions(memberId, performanceId, query)
    }
}
