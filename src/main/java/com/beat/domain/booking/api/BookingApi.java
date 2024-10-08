package com.beat.domain.booking.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.beat.domain.booking.application.dto.GuestBookingRequest;
import com.beat.domain.booking.application.dto.GuestBookingResponse;
import com.beat.domain.booking.application.dto.GuestBookingRetrieveRequest;
import com.beat.domain.booking.application.dto.GuestBookingRetrieveResponse;
import com.beat.domain.booking.application.dto.MemberBookingRequest;
import com.beat.domain.booking.application.dto.MemberBookingResponse;
import com.beat.domain.booking.application.dto.MemberBookingRetrieveResponse;
import com.beat.global.auth.annotation.CurrentMember;
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
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "회원 예매가 성공적으로 완료되었습니다.",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<MemberBookingResponse>> createMemberBooking(
		@CurrentMember Long memberId,
		@RequestBody MemberBookingRequest memberBookingRequest);

	@Operation(summary = "회원 예매 조회 API", description = "회원이 예매를 조회하는 GET API입니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "회원 예매 조회가 성공적으로 완료되었습니다.",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<List<MemberBookingRetrieveResponse>>> getMemberBookings(
		@CurrentMember Long memberId);

	@Operation(summary = "비회원 예매 API", description = "비회원이 예매를 요청하는 POST API입니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "비회원 예매가 성공적으로 완료되었습니다.",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<GuestBookingResponse>> createGuestBookings(
		@RequestBody GuestBookingRequest guestBookingRequest);

	@Operation(summary = "비회원 예매 조회 API", description = "비회원이 예매를 조회하는 POST API입니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "비회원 예매 조회가 성공적으로 완료되었습니다.",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<List<GuestBookingRetrieveResponse>>> getGuestBookings(
		@RequestBody GuestBookingRetrieveRequest guestBookingRetrieveRequest);
}
