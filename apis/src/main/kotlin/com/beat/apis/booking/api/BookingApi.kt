package com.beat.apis.booking.api

import com.beat.apis.booking.api.request.BookingCancelRequest
import com.beat.apis.booking.api.request.BookingRefundRequest
import com.beat.apis.booking.api.request.GuestBookingRequest
import com.beat.apis.booking.api.request.GuestBookingRetrieveRequest
import com.beat.apis.booking.api.request.MemberBookingRequest
import com.beat.apis.booking.api.response.BookingCancelResponse
import com.beat.apis.booking.api.response.BookingRefundResponse
import com.beat.apis.booking.api.response.GuestBookingResponse
import com.beat.apis.booking.api.response.GuestBookingRetrieveResponse
import com.beat.apis.booking.api.response.MemberBookingResponse
import com.beat.apis.booking.api.response.MemberBookingRetrieveResponse
import com.beat.apis.swagger.annotation.DisableSwaggerSecurity
import com.beat.gateway.CurrentMember
import com.beat.global.support.response.ErrorResponse
import com.beat.global.support.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Booking", description = "예매 관련 API")
interface BookingApi {

    @Operation(summary = "회원 예매 API", description = "회원이 예매를 요청하는 POST API입니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "회원 예매가 성공적으로 완료되었습니다."),
            ApiResponse(
                responseCode = "400",
                description = "필수 데이터가 누락되었거나 잘못된 요청 형식입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "회원, 공연 또는 회차 정보를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createMemberBooking(
        @CurrentMember memberId: Long,
        @RequestBody memberBookingRequest: MemberBookingRequest,
    ): ResponseEntity<SuccessResponse<MemberBookingResponse>>

    @Operation(summary = "회원 예매 조회 API", description = "회원이 예매를 조회하는 GET API입니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "회원 예매 조회가 성공적으로 완료되었습니다."),
            ApiResponse(
                responseCode = "404",
                description = "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getMemberBookings(
        @CurrentMember memberId: Long,
    ): ResponseEntity<SuccessResponse<List<MemberBookingRetrieveResponse>>>

    @DisableSwaggerSecurity
    @Operation(summary = "비회원 예매 API", description = "비회원이 예매를 요청하는 POST API입니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "비회원 예매가 성공적으로 완료되었습니다."),
            ApiResponse(
                responseCode = "400",
                description = "필수 데이터가 누락되었거나 잘못된 데이터 형식입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "공연 또는 회차 정보를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createGuestBookings(
        @RequestBody guestBookingRequest: GuestBookingRequest,
        httpServletResponse: HttpServletResponse,
    ): ResponseEntity<SuccessResponse<GuestBookingResponse>>

    @DisableSwaggerSecurity
    @Operation(summary = "비회원 예매 조회 API", description = "비회원이 예매를 조회하는 POST API입니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "비회원 예매 조회가 성공적으로 완료되었습니다."),
            ApiResponse(
                responseCode = "404",
                description = "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getGuestBookings(
        @RequestBody guestBookingRetrieveRequest: GuestBookingRetrieveRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ): ResponseEntity<SuccessResponse<List<GuestBookingRetrieveResponse>>>

    @DisableSwaggerSecurity
    @Operation(summary = "유료공연 예매 환불 요청 API", description = "회원 토큰 또는 비회원 예매 조회 후 발급된 세션이 필요합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "유료공연 예매 환불 요청이 성공적으로 완료되었습니다."),
            ApiResponse(
                responseCode = "404",
                description = "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun refundBookings(
        @CurrentMember memberId: Long?,
        @CookieValue(value = "guestSession", required = false) guestSessionToken: String?,
        @RequestBody bookingRefundRequest: BookingRefundRequest,
    ): ResponseEntity<SuccessResponse<BookingRefundResponse>>

    @DisableSwaggerSecurity
    @Operation(summary = "무료공연/미입금 예매 취소 요청 API", description = "회원 토큰 또는 비회원 예매 조회 후 발급된 세션이 필요합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "무료공연/미입금 예매 취소 요청이 성공적으로 완료되었습니다."),
            ApiResponse(
                responseCode = "404",
                description = "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun cancelBookings(
        @CurrentMember memberId: Long?,
        @CookieValue(value = "guestSession", required = false) guestSessionToken: String?,
        @RequestBody bookingCancelRequest: BookingCancelRequest,
    ): ResponseEntity<SuccessResponse<BookingCancelResponse>>
}
