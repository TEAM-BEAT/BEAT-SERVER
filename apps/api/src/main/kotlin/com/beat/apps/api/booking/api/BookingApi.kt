package com.beat.apps.api.booking.api

import com.beat.apps.api.booking.api.request.BookingCancelRequest
import com.beat.apps.api.booking.api.request.BookingRefundRequest
import com.beat.apps.api.booking.api.request.GuestBookingRequest
import com.beat.apps.api.booking.api.request.GuestBookingRetrieveRequest
import com.beat.apps.api.booking.api.request.MemberBookingRequest
import com.beat.apps.api.booking.api.response.BookingCancelResponse
import com.beat.apps.api.booking.api.response.BookingRefundResponse
import com.beat.apps.api.booking.api.response.GuestBookingResponse
import com.beat.apps.api.booking.api.response.GuestBookingRetrieveResponse
import com.beat.apps.api.booking.api.response.MemberBookingResponse
import com.beat.apps.api.booking.api.response.MemberBookingRetrieveResponse
import com.beat.apps.api.booking.web.GUEST_SESSION_COOKIE_NAME
import com.beat.apps.api.response.ErrorResponse
import com.beat.apps.api.response.SuccessResponse
import com.beat.apps.api.swagger.annotation.DisableSwaggerSecurity
import com.beat.support.security.CurrentMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
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

    @Operation(
        operationId = "createMemberBooking",
        summary = "회원 예매 생성",
        description =
            "Bearer 회원 토큰으로 인증된 회원이 공연 회차를 예매합니다. 무료 공연은 결제 금액이 0원이라 BOOKING_CONFIRMED 상태로 생성되고 계좌 필드는 null이며, 유료 공연은 CHECKING_PAYMENT 상태로 생성되고 공연에 등록된 은행·계좌번호 전체 값이 반환됩니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "201", description = "회원 예매가 생성되었습니다."),
                ApiResponse(
                    responseCode = "400",
                    description = "필수 값이 누락되었거나 예매자 정보 형식이 잘못되었거나 티켓 수량이 1~10장 범위를 벗어난 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "인증된 회원, 공연 또는 예매하려는 회차 정보를 찾을 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "409",
                    description = "예매 마감 시각이 지나 BOOKING_CLOSED 상태로 예매가 거부된 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun createMemberBooking(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @SwaggerRequestBody(
            description = "회원 예매 생성에 필요한 공연 회차, 티켓 수량, 예매자 정보를 담은 요청 본문입니다.",
            required = true,
        )
        @RequestBody
        memberBookingRequest: MemberBookingRequest,
    ): ResponseEntity<SuccessResponse<MemberBookingResponse>>

    @Operation(
        operationId = "getMemberBookings",
        summary = "회원 예매 내역 조회",
        description =
            "Bearer 회원 토큰으로 인증된 회원의 예매 내역을 조회합니다. 무료 공연의 계좌 필드는 null이고, 유료 공연의 계좌 필드는 공연에 등록된 전체 값으로 반환됩니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "회원 예매 내역이 조회되었습니다."),
                ApiResponse(
                    responseCode = "404",
                    description = "회원 또는 회원 예매에 연결된 공연·회차 정보를 찾을 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun getMemberBookings(
        @Parameter(hidden = true) @CurrentMember memberId: Long
    ): ResponseEntity<SuccessResponse<List<MemberBookingRetrieveResponse>>>

    @DisableSwaggerSecurity
    @Operation(
        operationId = "createGuestBooking",
        summary = "비회원 예매 생성",
        description =
            "회원 토큰 없이 비회원 예매자 정보로 공연 회차를 예매합니다. 무료 공연은 결제 금액이 0원이라 BOOKING_CONFIRMED 상태로 생성되고 계좌 필드는 null이며, 유료 공연은 CHECKING_PAYMENT 상태로 생성되고 공연에 등록된 은행·계좌번호 전체 값이 반환됩니다. 게스트 세션 발급에 성공하면 __Host-guestSession 쿠키를 응답에 설정합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "201", description = "비회원 예매가 생성되었습니다."),
                ApiResponse(
                    responseCode = "400",
                    description =
                        "필수 값이 누락되었거나 이름·전화번호·생년월일·비밀번호 형식이 잘못되었거나 티켓 수량이 1~10장 범위를 벗어난 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "예매하려는 공연 또는 회차 정보를 찾을 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "409",
                    description = "예매 마감 시각이 지나 BOOKING_CLOSED 상태로 예매가 거부된 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun createGuestBookings(
        @SwaggerRequestBody(
            description = "비회원 예매 생성에 필요한 공연 회차, 티켓 수량, 예매자 본인 확인 정보를 담은 요청 본문입니다.",
            required = true,
        )
        @RequestBody
        guestBookingRequest: GuestBookingRequest,
        @Parameter(hidden = true) httpServletResponse: HttpServletResponse,
    ): ResponseEntity<SuccessResponse<GuestBookingResponse>>

    @DisableSwaggerSecurity
    @Operation(
        operationId = "getGuestBookings",
        summary = "비회원 예매 내역 조회",
        description =
            "회원 토큰 없이 예매 시 입력한 이름·전화번호·생년월일·비밀번호로 비회원 예매 내역을 조회합니다. 게스트 세션 발급에 성공하면 __Host-guestSession 쿠키를 응답에 설정합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "비회원 예매 내역이 조회되었습니다."),
                ApiResponse(
                    responseCode = "400",
                    description = "예매자 본인 확인 정보가 누락되었거나 이름·전화번호·생년월일·비밀번호 형식이 잘못된 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "인증 정보와 일치하는 예매가 없거나 예매에 연결된 공연·회차 정보를 찾을 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "429",
                    description = "비회원 예매 조회 요청이 허용된 횟수를 초과해 일시적으로 제한된 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun getGuestBookings(
        @SwaggerRequestBody(
            description = "비회원 예매 조회에 사용할 예매자 본인 확인 정보를 담은 요청 본문입니다.",
            required = true,
        )
        @RequestBody
        guestBookingRetrieveRequest: GuestBookingRetrieveRequest,
        @Parameter(hidden = true) httpServletRequest: HttpServletRequest,
        @Parameter(hidden = true) httpServletResponse: HttpServletResponse,
    ): ResponseEntity<SuccessResponse<List<GuestBookingRetrieveResponse>>>

    @DisableSwaggerSecurity
    @Operation(
        operationId = "requestBookingRefund",
        summary = "유료 공연 예매 환불 요청",
        description =
            "회원 access token 또는 비회원 예매 조회 후 발급된 __Host-guestSession 쿠키 중 하나로 예매자를 인증해 유료 공연 예매의 환불을 요청합니다. 두 인증 수단이 모두 없으면 401, 게스트 쿠키 요청의 Origin이 허용되지 않으면 403(응답 본문 없음)입니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "유료 공연 예매 환불 요청이 접수되었습니다."),
                ApiResponse(
                    responseCode = "400",
                    description = "환불 계좌 정보가 누락되었거나 은행·계좌번호·예금주 정보가 유효하지 않은 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "401",
                    description =
                        "회원 access token과 __Host-guestSession 쿠키가 모두 없어 예매자 인증이 필요한 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "403",
                    description = "게스트 세션 쿠키를 사용한 요청의 Origin이 허용된 출처가 아닌 경우이며 응답 본문은 없습니다.",
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "예매를 찾을 수 없거나 인증된 예매자가 해당 예매의 소유자가 아닌 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "409",
                    description = "현재 예매 상태가 환불 요청을 허용하지 않는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun refundBookings(
        @Parameter(hidden = true) @CurrentMember memberId: Long?,
        @Parameter(
            name = GUEST_SESSION_COOKIE_NAME,
            `in` = ParameterIn.COOKIE,
            description = "비회원 예매자 인증에 사용하는 게스트 세션 쿠키입니다. 회원 access token이 없을 때 사용합니다.",
            example = "guest-session-example",
            required = false,
        )
        @CookieValue(value = GUEST_SESSION_COOKIE_NAME, required = false)
        guestSessionToken: String?,
        @SwaggerRequestBody(
            description = "환불할 예매 ID와 환불받을 은행·계좌 정보를 담은 요청 본문입니다. 환불 계좌 정보는 모두 필수입니다.",
            required = true,
        )
        @RequestBody
        bookingRefundRequest: BookingRefundRequest,
    ): ResponseEntity<SuccessResponse<BookingRefundResponse>>

    @DisableSwaggerSecurity
    @Operation(
        operationId = "requestBookingCancellation",
        summary = "무료 공연·미입금 예매 취소 요청",
        description =
            "회원 access token 또는 비회원 예매 조회 후 발급된 __Host-guestSession 쿠키 중 하나로 예매자를 인증해 무료 공연 또는 미입금 예매의 취소를 요청합니다. 두 인증 수단이 모두 없으면 401, 게스트 쿠키 요청의 Origin이 허용되지 않으면 403(응답 본문 없음)입니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "무료 공연 또는 미입금 예매 취소 요청이 처리되었습니다.",
                ),
                ApiResponse(
                    responseCode = "401",
                    description =
                        "회원 access token과 __Host-guestSession 쿠키가 모두 없어 예매자 인증이 필요한 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "403",
                    description = "게스트 세션 쿠키를 사용한 요청의 Origin이 허용된 출처가 아닌 경우이며 응답 본문은 없습니다.",
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "예매를 찾을 수 없거나 인증된 예매자가 해당 예매의 소유자가 아닌 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "409",
                    description = "입금이 확인된 유료 예매·환불 처리 중인 예매 등 현재 상태에서 직접 취소할 수 없는 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun cancelBookings(
        @Parameter(hidden = true) @CurrentMember memberId: Long?,
        @Parameter(
            name = GUEST_SESSION_COOKIE_NAME,
            `in` = ParameterIn.COOKIE,
            description = "비회원 예매자 인증에 사용하는 게스트 세션 쿠키입니다. 회원 access token이 없을 때 사용합니다.",
            example = "guest-session-example",
            required = false,
        )
        @CookieValue(value = GUEST_SESSION_COOKIE_NAME, required = false)
        guestSessionToken: String?,
        @SwaggerRequestBody(
            description = "취소할 예매 ID를 담은 요청 본문입니다.",
            required = true,
        )
        @RequestBody
        bookingCancelRequest: BookingCancelRequest,
    ): ResponseEntity<SuccessResponse<BookingCancelResponse>>
}
