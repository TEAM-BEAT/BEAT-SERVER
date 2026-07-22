package com.beat.apis.ticket.api

import com.beat.apis.booking.api.type.BookingStatusType
import com.beat.apis.schedule.api.type.ScheduleNumberType
import com.beat.apis.ticket.api.request.TicketDeleteRequest
import com.beat.apis.ticket.api.request.TicketRefundRequest
import com.beat.apis.ticket.api.request.TicketUpdateRequest
import com.beat.apis.ticket.api.response.TicketRetrieveResponse
import com.beat.gateway.security.servlet.CurrentMember
import com.beat.global.support.response.ErrorResponse
import com.beat.global.support.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Ticket", description = "티켓 관련 API")
interface TicketApi {

    @Operation(summary = "예매자 목록 조회 API", description = "메이커가 자신의 공연에 대한 예매자 목록을 조회하는 GET API입니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "예매자 목록 조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "공연 또는 회차 정보를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getTickets(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
        @RequestParam(required = false) scheduleNumbers: List<ScheduleNumberType>?,
        @RequestParam(required = false) bookingStatuses: List<BookingStatusType>?,
    ): ResponseEntity<SuccessResponse<TicketRetrieveResponse>>

    @Operation(summary = "예매자 목록 검색 API", description = "메이커가 자신의 공연에 대한 예매자 목록을 검색하는 GET API입니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "예매자 목록 검색 성공"),
            ApiResponse(
                responseCode = "404",
                description = "공연 또는 회차 정보를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun searchTickets(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
        @RequestParam searchWord: String,
        @RequestParam(required = false) scheduleNumbers: List<ScheduleNumberType>?,
        @RequestParam(required = false) bookingStatuses: List<BookingStatusType>?,
    ): ResponseEntity<SuccessResponse<TicketRetrieveResponse>>

    @Operation(
        summary = "예매자 입금여부 수정 및 웹발신 API",
        description = "메이커가 자신의 공연에 대한 예매자의 입금여부 정보를 수정한 뒤 예매확정 웹발신을 보내는 PUT API입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "예매자 입금여부 수정 성공"),
            ApiResponse(
                responseCode = "400",
                description = "이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateTickets(
        @CurrentMember memberId: Long,
        @RequestBody request: TicketUpdateRequest,
    ): ResponseEntity<SuccessResponse<Void>>

    @Operation(
        summary = "예매자 환불처리 API",
        description = "메이커가 자신의 공연에 대한 1명 이상의 예매자의 정보를 환불완료 상태로 변경하는 PUT API입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "예매자 환불처리 성공"),
            ApiResponse(
                responseCode = "404",
                description = "해당 예매 내역을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun refundTickets(
        @CurrentMember memberId: Long,
        @RequestBody ticketRefundRequest: TicketRefundRequest,
    ): ResponseEntity<SuccessResponse<Void>>

    @Operation(
        summary = "예매자 삭제 API",
        description = "메이커가 자신의 공연에 대한 1명 이상의 예매자의 정보를 삭제하는 PUT API입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "예매자 삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "해당 예매 내역을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteTickets(
        @CurrentMember memberId: Long,
        @RequestBody ticketDeleteRequest: TicketDeleteRequest,
    ): ResponseEntity<SuccessResponse<Void>>
}
