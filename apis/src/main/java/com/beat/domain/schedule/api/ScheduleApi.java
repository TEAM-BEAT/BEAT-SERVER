package com.beat.domain.schedule.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.beat.domain.schedule.application.dto.response.TicketAvailabilityResponse;
import com.beat.global.common.dto.ErrorResponse;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.swagger.annotation.DisableSwaggerSecurity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Schedule", description = "스케줄 관련 API")
public interface ScheduleApi {

	@DisableSwaggerSecurity
	@Operation(summary = "티켓 구매 가능 여부 조회 API", description = "티켓 구매 가능 여부를 확인하는 GET API입니다.")
	@ApiResponses(
		value = {
			@ApiResponse(
				responseCode = "200",
				description = "티켓 수량 조회가 성공적으로 완료되었습니다."
			),
			@ApiResponse(
				responseCode = "400",
				description = "잘못된 데이터 형식입니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "404",
				description = "회차 정보를 찾을 수 없습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
				responseCode = "409",
				description = "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다.",
				content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
		}
	)
	ResponseEntity<SuccessResponse<TicketAvailabilityResponse>> getTicketAvailability(
		@PathVariable Long scheduleId,
		@RequestParam int purchaseTicketCount
	);
}
