package com.beat.domain.booking.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.beat.domain.booking.application.dto.TicketCancelRequest;
import com.beat.domain.booking.application.dto.TicketRetrieveResponse;
import com.beat.domain.booking.application.dto.TicketUpdateRequest;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ticket", description = "티켓 관련 API")
public interface TicketApi {

	@Operation(summary = "예매자 목록 조회 API", description = "메이커가 자신의 공연에 대한 예매자 목록을 조회하는 GET API입니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "예매자 목록 조회 성공",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<TicketRetrieveResponse>> getTickets(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId,
		@RequestParam(required = false) ScheduleNumber scheduleNumber,
		@RequestParam(required = false) BookingStatus bookingStatus);

	@Operation(summary = "예매자 입금여부 수정 및 웹발신 API", description = "메이커가 자신의 공연에 대한 예매자의 입금여부 정보를 수정한 뒤 예매확정 웹발신을 보내는 PUT API입니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "예매자 입금여부 수정 성공",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<Void>> updateTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketUpdateRequest request);

	@Operation(summary = "예매자 취소 API", description = "메이커가 자신의 공연에 대한 1명 이상의 예매자의 정보를 취소 상태로 변경하는 PATCH API입니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "예매자 취소 성공",
			content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
	})
	ResponseEntity<SuccessResponse<Void>> cancelTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketCancelRequest ticketCancelRequest);
}
