package com.beat.apis.schedule.facade;

import org.springframework.stereotype.Service;

import com.beat.apis.schedule.application.ScheduleService;
import com.beat.apis.schedule.application.dto.request.TicketAvailabilityRequest;
import com.beat.apis.schedule.application.dto.response.TicketAvailabilityResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleFacade {
	private final ScheduleService scheduleService;

	public TicketAvailabilityResponse findTicketAvailability(Long scheduleId, int purchaseTicketCount) {
		return scheduleService.findTicketAvailability(scheduleId, TicketAvailabilityRequest.of(purchaseTicketCount));
	}
}
