package com.beat.domain.schedule.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beat.domain.schedule.application.ScheduleService;
import com.beat.domain.schedule.application.dto.TicketAvailabilityRequest;
import com.beat.domain.schedule.application.dto.TicketAvailabilityResponse;
import com.beat.domain.schedule.exception.ScheduleSuccessCode;
import com.beat.global.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApi {

	private final ScheduleService scheduleService;

	@Override
	@GetMapping("/{scheduleId}/availability")
	public ResponseEntity<SuccessResponse<TicketAvailabilityResponse>> getTicketAvailability(
		@PathVariable Long scheduleId,
		@RequestParam int purchaseTicketCount) {

		TicketAvailabilityRequest ticketAvailabilityRequest = TicketAvailabilityRequest.of(purchaseTicketCount);
		TicketAvailabilityResponse response = scheduleService.findTicketAvailability(scheduleId,
			ticketAvailabilityRequest);

		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(ScheduleSuccessCode.TICKET_AVAILABILITY_RETRIEVAL_SUCCESS, response));
	}
}