package com.beat.domain.booking.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.beat.domain.booking.application.dto.TicketDeleteRequest;
import com.beat.domain.booking.application.dto.TicketRefundRequest;
import com.beat.domain.booking.application.dto.TicketRetrieveResponse;
import com.beat.domain.booking.application.dto.TicketUpdateRequest;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.ErrorResponse;
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
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "예매자 목록 조회 성공"
			),
			@ApiResponse(
				responseCode = "404",
				description = "입력하신 정보와 일치하는 예매자 목록이 없습니다.",
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
	ResponseEntity<SuccessResponse<TicketRetrieveResponse>> getTickets(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId,
		@RequestParam(required = false) List<ScheduleNumber> scheduleNumbers,
		@RequestParam(required = false) List<BookingStatus> bookingStatuses
	);

	@Operation(summary = "예매자 목록 검색 API", description = "메이커가 자신의 공연에 대한 예매자 목록을 검색하는 GET API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "예매자 목록 검색 성공"
			),
			@ApiResponse(
				responseCode = "404",
				description = "입력하신 정보와 일치하는 예매자 목록이 없습니다.",
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
	ResponseEntity<SuccessResponse<TicketRetrieveResponse>> searchTickets(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId,
		@RequestParam String searchWord,
		@RequestParam(required = false) List<ScheduleNumber> scheduleNumbers,
		@RequestParam(required = false) List<BookingStatus> bookingStatuses
	);

	@Operation(summary = "예매자 입금여부 수정 및 웹발신 API", description = "메이커가 자신의 공연에 대한 예매자의 입금여부 정보를 수정한 뒤 예매확정 웹발신을 보내는 PUT API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "예매자 입금여부 수정 성공"
			),
			@ApiResponse(
				responseCode = "400",
				description = "이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다.",
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
	ResponseEntity<SuccessResponse<Void>> updateTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketUpdateRequest request
	);

	@Operation(summary = "예매자 환불처리 API", description = "메이커가 자신의 공연에 대한 1명 이상의 예매자의 정보를 환불완료 상태로 변경하는 PUT API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "예매자 환불처리 성공"
			),
			@ApiResponse(
				responseCode = "404",
				description = "해당 예매 내역을 찾을 수 없습니다.",
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
	ResponseEntity<SuccessResponse<Void>> refundTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketRefundRequest ticketRefundRequest
	);

	@Operation(summary = "예매자 삭제 API", description = "메이커가 자신의 공연에 대한 1명 이상의 예매자의 정보를 삭제하는 PUT API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "예매자 삭제 성공"
			),
			@ApiResponse(
				responseCode = "404",
				description = "해당 예매 내역을 찾을 수 없습니다.",
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
	ResponseEntity<SuccessResponse<Void>> deleteTickets(
		@CurrentMember Long memberId,
		@RequestBody TicketDeleteRequest ticketDeleteRequest
	);

}
