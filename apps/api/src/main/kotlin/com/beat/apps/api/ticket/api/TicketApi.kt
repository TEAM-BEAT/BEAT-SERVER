package com.beat.apps.api.ticket.api

import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.response.ErrorResponse
import com.beat.apps.api.response.SuccessResponse
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import com.beat.apps.api.ticket.api.request.TicketDeleteRequest
import com.beat.apps.api.ticket.api.request.TicketRefundRequest
import com.beat.apps.api.ticket.api.request.TicketUpdateRequest
import com.beat.apps.api.ticket.api.response.TicketRetrieveResponse
import com.beat.support.security.CurrentMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Ticket", description = "티켓 관련 API")
interface TicketApi {

    @Operation(
        operationId = "ticketRetrieveForMaker",
        summary = "공연 예매자 목록 조회",
        description = "메이커가 소유한 공연의 예매자 목록을 회차와 예매 상태로 필터링해 조회합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "예매자 목록 조회 성공"),
                ApiResponse(
                    responseCode = "400",
                    description =
                        "회차 번호 또는 예매 상태 enum 값이 올바르지 않거나, 삭제된 예매 상태(BOOKING_DELETED)를 조회 조건으로 지정했습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "403",
                    description = "해당 공연의 메이커가 아니어서 예매자 목록을 조회할 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원, 공연 또는 회차 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun getTickets(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @Parameter(
            description = "예매자 목록을 조회할 공연 식별자입니다.",
            example = "100",
            required = true,
        )
        @PathVariable
        performanceId: Long,
        @Parameter(
            description = "조회할 회차 번호 목록입니다. 생략하면 모든 회차를 조회합니다.",
            example = "FIRST",
            required = false,
        )
        @RequestParam(required = false)
        scheduleNumbers: List<ScheduleNumberType>?,
        @Parameter(
            description = "조회할 예매 상태 목록입니다. 생략하면 삭제되지 않은 예매 상태를 조회하며 BOOKING_DELETED는 조회할 수 없습니다.",
            example = "CHECKING_PAYMENT",
            required = false,
        )
        @RequestParam(required = false)
        bookingStatuses: List<BookingStatusType>?,
    ): ResponseEntity<SuccessResponse<TicketRetrieveResponse>>

    @Operation(
        operationId = "ticketSearchForMaker",
        summary = "공연 예매자 목록 검색",
        description = "메이커가 소유한 공연의 예매자 이름을 검색어로 사용해 예매자 목록을 조회합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "예매자 목록 검색 성공"),
                ApiResponse(
                    responseCode = "400",
                    description =
                        "회차 번호 또는 예매 상태 enum 값이 올바르지 않거나, 삭제된 예매 상태(BOOKING_DELETED)를 지정했거나, 검색어가 2글자 미만입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "403",
                    description = "해당 공연의 메이커가 아니어서 예매자 목록을 검색할 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원, 공연 또는 회차 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun searchTickets(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @Parameter(
            description = "예매자 목록을 검색할 공연 식별자입니다.",
            example = "100",
            required = true,
        )
        @PathVariable
        performanceId: Long,
        @Parameter(
            description = "예매자 이름 검색어입니다. 공백을 제거한 뒤 최소 2글자 이상이어야 합니다.",
            example = "booker",
            required = true,
        )
        @RequestParam
        searchWord: String,
        @Parameter(
            description = "검색할 회차 번호 목록입니다. 생략하면 모든 회차를 검색합니다.",
            example = "FIRST",
            required = false,
        )
        @RequestParam(required = false)
        scheduleNumbers: List<ScheduleNumberType>?,
        @Parameter(
            description = "검색할 예매 상태 목록입니다. 생략하면 환불 요청·입금 확인 중·예매 확정·예매 취소 상태를 검색합니다.",
            example = "CHECKING_PAYMENT",
            required = false,
        )
        @RequestParam(required = false)
        bookingStatuses: List<BookingStatusType>?,
    ): ResponseEntity<SuccessResponse<TicketRetrieveResponse>>

    @Operation(
        operationId = "ticketUpdatePaymentStatusForMaker",
        summary = "예매자 입금 여부 수정 및 웹발신",
        description = "메이커가 자신의 공연에 속한 예매자의 입금 상태를 일괄 수정하고 예매 확정 웹발신을 보냅니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "예매자 입금여부 수정 성공"),
                ApiResponse(
                    responseCode = "400",
                    description = "요청 형식이 올바르지 않거나, 중복된 예매 식별자 또는 지원하지 않는 예매 상태가 포함되어 있습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "403",
                    description = "해당 공연의 메이커가 아니거나, 예매에 연결된 회차가 해당 공연에 속하지 않습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원, 공연, 예매 또는 회차 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "409",
                    description = "이미 결제가 완료된 예매이거나 허용되지 않은 상태 전이로 인해 입금 상태를 변경할 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun updateTickets(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @SwaggerRequestBody(
            description = "입금 확인 상태로 변경할 예매자 목록을 포함한 요청 본문입니다.",
            required = true,
        )
        @RequestBody
        request: TicketUpdateRequest,
    ): ResponseEntity<SuccessResponse<Void?>>

    @Operation(
        operationId = "ticketRefundForMaker",
        summary = "예매자 환불 완료 처리",
        description = "메이커가 자신의 공연에 속한 예매자를 환불 완료 상태로 처리합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "예매자 환불처리 성공"),
                ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식이 올바르지 않거나 예매 식별자 형식이 잘못되었습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "403",
                    description = "해당 공연의 메이커가 아니거나, 예매에 연결된 회차가 해당 공연에 속하지 않습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원, 공연, 예매 또는 회차 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "409",
                    description = "환불 요청(REFUND_REQUESTED) 상태인 예매만 환불 완료 처리할 수 있습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun refundTickets(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @SwaggerRequestBody(
            description = "환불 요청 상태인 예매자를 환불 완료 처리할 공연과 예매 식별자 목록입니다.",
            required = true,
        )
        @RequestBody
        ticketRefundRequest: TicketRefundRequest,
    ): ResponseEntity<SuccessResponse<Void?>>

    @Operation(
        operationId = "ticketDeleteForMaker",
        summary = "예매자 삭제",
        description = "메이커가 자신의 공연에 속한 삭제 가능한 예매자를 삭제 처리합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "예매자 삭제 성공"),
                ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식이 올바르지 않거나 예매 식별자 형식이 잘못되었습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "403",
                    description = "해당 공연의 메이커가 아니거나, 예매에 연결된 회차가 해당 공연에 속하지 않습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원, 공연, 예매 또는 회차 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "409",
                    description =
                        "삭제 가능한 상태가 아닌 예매가 포함되어 있습니다. CHECKING_PAYMENT, 결제 금액이 0원인 BOOKING_CONFIRMED 또는 BOOKING_CANCELLED 상태만 삭제할 수 있습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun deleteTickets(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @SwaggerRequestBody(
            description = "삭제할 공연과 예매 식별자 목록을 포함한 요청 본문입니다.",
            required = true,
        )
        @RequestBody
        ticketDeleteRequest: TicketDeleteRequest,
    ): ResponseEntity<SuccessResponse<Void?>>
}
