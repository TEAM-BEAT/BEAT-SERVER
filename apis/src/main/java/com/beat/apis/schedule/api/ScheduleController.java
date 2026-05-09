package com.beat.apis.schedule.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beat.apis.schedule.application.dto.response.TicketAvailabilityResponse;
import com.beat.apis.schedule.api.response.ScheduleSuccessCode;
import com.beat.apis.schedule.facade.ScheduleFacade;
import com.beat.global.support.response.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApi {

	private final ScheduleFacade scheduleFacade;

	@Override
	@GetMapping("/{scheduleId}/availability")
	public ResponseEntity<SuccessResponse<TicketAvailabilityResponse>> getTicketAvailability(
		@PathVariable Long scheduleId,
		@RequestParam int purchaseTicketCount) {

		TicketAvailabilityResponse response = scheduleFacade.findTicketAvailability(scheduleId, purchaseTicketCount);

		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(ScheduleSuccessCode.TICKET_AVAILABILITY_RETRIEVAL_SUCCESS, response));
	}
}
