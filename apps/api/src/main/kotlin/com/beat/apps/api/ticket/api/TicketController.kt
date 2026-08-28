package com.beat.apps.api.ticket.api

import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.response.SuccessResponse
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import com.beat.apps.api.ticket.api.request.TicketDeleteRequest
import com.beat.apps.api.ticket.api.request.TicketRefundRequest
import com.beat.apps.api.ticket.api.request.TicketUpdateRequest
import com.beat.apps.api.ticket.api.response.TicketRetrieveResponse
import com.beat.apps.api.ticket.api.response.TicketSuccessCode
import com.beat.apps.api.ticket.facade.TicketFacade
import com.beat.support.security.CurrentMember
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tickets")
class TicketController(private val ticketFacade: TicketFacade) : TicketApi {

    @GetMapping("/{performanceId}")
    override fun getTickets(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
        @RequestParam(required = false) scheduleNumbers: List<ScheduleNumberType>?,
        @RequestParam(required = false) bookingStatuses: List<BookingStatusType>?,
    ): ResponseEntity<SuccessResponse<TicketRetrieveResponse>> {
        val response =
            ticketFacade.findTickets(memberId, performanceId, scheduleNumbers, bookingStatuses)
        return ResponseEntity.ok(
            SuccessResponse.of(TicketSuccessCode.TICKET_RETRIEVE_SUCCESS, response)
        )
    }

    @GetMapping("/search/{performanceId}")
    override fun searchTickets(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
        @RequestParam searchWord: String,
        @RequestParam(required = false) scheduleNumbers: List<ScheduleNumberType>?,
        @RequestParam(required = false) bookingStatuses: List<BookingStatusType>?,
    ): ResponseEntity<SuccessResponse<TicketRetrieveResponse>> {
        val response =
            ticketFacade.searchTickets(
                memberId,
                performanceId,
                searchWord,
                scheduleNumbers,
                bookingStatuses,
            )
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .body(SuccessResponse.of(TicketSuccessCode.TICKET_SEARCH_SUCCESS, response))
    }

    @PutMapping("/update")
    override fun updateTickets(
        @CurrentMember memberId: Long,
        @Valid @RequestBody request: TicketUpdateRequest,
    ): ResponseEntity<SuccessResponse<Void?>> {
        ticketFacade.updateTickets(memberId, request)
        return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_UPDATE_SUCCESS))
    }

    @PutMapping("/refund")
    override fun refundTickets(
        @CurrentMember memberId: Long,
        @Valid @RequestBody ticketRefundRequest: TicketRefundRequest,
    ): ResponseEntity<SuccessResponse<Void?>> {
        ticketFacade.refundTickets(memberId, ticketRefundRequest)
        return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_REFUND_SUCCESS))
    }

    @PutMapping("/delete")
    override fun deleteTickets(
        @CurrentMember memberId: Long,
        @Valid @RequestBody ticketDeleteRequest: TicketDeleteRequest,
    ): ResponseEntity<SuccessResponse<Void?>> {
        ticketFacade.deleteTickets(memberId, ticketDeleteRequest)
        return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_DELETE_SUCCESS))
    }
}
