package com.beat.domain.booking.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.beat.domain.booking.application.dto.BookingCancelRequest;
import com.beat.domain.booking.application.dto.BookingCancelResponse;
import com.beat.domain.booking.application.dto.BookingRefundRequest;
import com.beat.domain.booking.application.dto.BookingRefundResponse;
import com.beat.domain.booking.application.dto.GuestBookingRequest;
import com.beat.domain.booking.application.dto.GuestBookingResponse;
import com.beat.domain.booking.application.dto.GuestBookingRetrieveRequest;
import com.beat.domain.booking.application.dto.GuestBookingRetrieveResponse;
import com.beat.domain.booking.application.dto.MemberBookingRequest;
import com.beat.domain.booking.application.dto.MemberBookingResponse;
import com.beat.domain.booking.application.dto.MemberBookingRetrieveResponse;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.ErrorResponse;
import com.beat.global.common.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Booking", description = "예매 관련 API")
public interface BookingApi {

	@Operation(summary = "회원 예매 API", description = "회원이 예매를 요청하는 POST API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "201",
				description = "회원 예매가 성공적으로 완료되었습니다."
			),
			@ApiResponse(
				responseCode = "400",
				description = "필수 데이터가 누락되었습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "400",
				description = "잘못된 데이터 형식입니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "400",
				description = "잘못된 요청 형식입니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "404",
				description = "회원 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "404",
				description = "공연 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "404",
				description = "회차 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<MemberBookingResponse>> createMemberBooking(
		@CurrentMember Long memberId,
		@RequestBody MemberBookingRequest memberBookingRequest
	);

	@Operation(summary = "회원 예매 조회 API", description = "회원이 예매를 조회하는 GET API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "회원 예매 조회가 성공적으로 완료되었습니다."
			),
			@ApiResponse(
				responseCode = "404",
				description = "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<List<MemberBookingRetrieveResponse>>> getMemberBookings(
		@CurrentMember Long memberId
	);

	@Operation(summary = "비회원 예매 API", description = "비회원이 예매를 요청하는 POST API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "201",
				description = "비회원 예매가 성공적으로 완료되었습니다."
			),
			@ApiResponse(
				responseCode = "400",
				description = "필수 데이터가 누락되었습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "400",
				description = "잘못된 데이터 형식입니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "404",
				description = "공연 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "404",
				description = "회차 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<GuestBookingResponse>> createGuestBookings(
		@RequestBody GuestBookingRequest guestBookingRequest
	);

	@Operation(summary = "비회원 예매 조회 API", description = "비회원이 예매를 조회하는 POST API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "비회원 예매 조회가 성공적으로 완료되었습니다."
			),
			@ApiResponse(
				responseCode = "404",
				description = "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<List<GuestBookingRetrieveResponse>>> getGuestBookings(
		@RequestBody GuestBookingRetrieveRequest guestBookingRetrieveRequest
	);

	@Operation(summary = "유료공연 예매 환불 요청 API", description = "유료공연 예매자가 환불 요청하는 PATCH API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "유료공연 예매 환불 요청이 성공적으로 완료되었습니다."
			),
			@ApiResponse(
				responseCode = "404",
				description = "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<BookingRefundResponse>> refundBookings(
		@RequestBody BookingRefundRequest bookingRefundRequest
	);

	@Operation(summary = "무료공연/미입금 예매 취소 요청 API", description = "무료공연/미입금 예매자가 취소 요청하는 PATCH API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "무료공연/미입금 예매 취소 요청이 성공적으로 완료되었습니다."
			),
			@ApiResponse(
				responseCode = "404",
				description = "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<BookingCancelResponse>> cancelBookings(
		@RequestBody BookingCancelRequest bookingCancelRequest
	);
}
