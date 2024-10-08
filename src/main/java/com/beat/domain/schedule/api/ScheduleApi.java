package com.beat.domain.schedule.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.beat.domain.schedule.application.dto.TicketAvailabilityResponse;
import com.beat.global.common.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Schedule", description = "스케줄 관련 API")
public interface ScheduleApi {

	@Operation(summary = "티켓 구매 가능 여부 조회 API", description = "티켓 구매 가능 여부를 확인하는 GET API입니다.")
	ResponseEntity<SuccessResponse<TicketAvailabilityResponse>> getTicketAvailability(
		@PathVariable Long scheduleId,
		@RequestParam int purchaseTicketCount);
}
