package com.beat.apis.booking.api

import com.beat.apis.booking.application.dto.TicketDeleteRequest
import com.beat.apis.booking.application.dto.TicketRefundRequest
import com.beat.apis.booking.application.dto.TicketRetrieveResponse
import com.beat.apis.booking.application.dto.TicketUpdateRequest
import com.beat.apis.booking.facade.TicketFacade
import com.beat.domain.booking.domain.BookingStatus
import com.beat.domain.booking.exception.TicketErrorCode
import com.beat.domain.booking.exception.TicketSuccessCode
import com.beat.domain.schedule.domain.ScheduleNumber
import com.beat.gateway.annotation.CurrentMember
import com.beat.global.common.dto.SuccessResponse
import com.beat.global.common.exception.BadRequestException
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
@RequestMapping("/api/v2/tickets")
class TicketV2Controller
    private val ticketFacade: TicketFacade,
) : TicketV2Api {

    @GetMapping("/{performanceId}")
    override fun getTickets(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
        @RequestParam(required = false) scheduleNumbers: List<ScheduleNumber>?,
        @RequestParam(required = false) bookingStatuses: List<BookingStatus>?,
    ): ResponseEntity<SuccessResponse<TicketRetrieveResponse>> {
        validateDeletedTicketRetrieveNotAllowed(bookingStatuses)

        val response = ticketFacade.findAllTicketsByConditions(
            memberId = memberId,
            performanceId = performanceId,
            scheduleNumbers = scheduleNumbers,
            bookingStatuses = bookingStatuses,
        )

        return ResponseEntity.ok(
            SuccessResponse.of(TicketSuccessCode.TICKET_RETRIEVE_SUCCESS, response),
        )
    }

    @GetMapping("/search/{performanceId}")
    override fun searchTickets(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
        @RequestParam searchWord: String,
        @RequestParam(required = false) scheduleNumbers: List<ScheduleNumber>?,
        @RequestParam(required = false) bookingStatuses: List<BookingStatus>?,
    ): ResponseEntity<SuccessResponse<TicketRetrieveResponse>> {
        if (searchWord.length < MIN_SEARCH_WORD_LENGTH) {
            throw BadRequestException(TicketErrorCode.SEARCH_WORD_TOO_SHORT)
        }
        validateDeletedTicketRetrieveNotAllowed(bookingStatuses)

        val response = ticketFacade.searchAllTicketsByConditions(
            memberId = memberId,
            performanceId = performanceId,
            searchWord = searchWord,
            scheduleNumbers = scheduleNumbers,
            bookingStatuses = bookingStatuses,
        )

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .body(SuccessResponse.of(TicketSuccessCode.TICKET_SEARCH_SUCCESS, response))
    }

    @PutMapping("/update")
    override fun updateTickets(
        @CurrentMember memberId: Long,
        @RequestBody ticketUpdateRequest: TicketUpdateRequest,
    ): ResponseEntity<SuccessResponse<Void>> {
        ticketFacade.updateTickets(memberId, ticketUpdateRequest)
        return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_UPDATE_SUCCESS))
    }

    @PutMapping("/refund")
    override fun refundTickets(
        @CurrentMember memberId: Long,
        @RequestBody ticketRefundRequest: TicketRefundRequest,
    ): ResponseEntity<SuccessResponse<Void>> {
        ticketFacade.refundTicketsByBookingIds(memberId, ticketRefundRequest)
        return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_REFUND_SUCCESS))
    }

    @PutMapping("/delete")
    override fun deleteTickets(
        @CurrentMember memberId: Long,
        @RequestBody ticketDeleteRequest: TicketDeleteRequest,
    ): ResponseEntity<SuccessResponse<Void>> {
        ticketFacade.deleteTicketsByBookingIds(memberId, ticketDeleteRequest)
        return ResponseEntity.ok(SuccessResponse.from(TicketSuccessCode.TICKET_DELETE_SUCCESS))
    }

    private fun validateDeletedTicketRetrieveNotAllowed(bookingStatuses: List<BookingStatus>?) {
        if (bookingStatuses?.contains(BookingStatus.BOOKING_DELETED) == true) {
            throw BadRequestException(TicketErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED)
        }
    }

    companion object {
        private const val MIN_SEARCH_WORD_LENGTH = 2
    }
}