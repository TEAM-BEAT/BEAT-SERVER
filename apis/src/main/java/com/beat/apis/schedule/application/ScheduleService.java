package com.beat.apis.schedule.application;

import com.beat.apis.schedule.application.dto.request.TicketAvailabilityRequest;
import com.beat.apis.schedule.application.dto.response.MinPerformanceDateResponse;
import com.beat.apis.schedule.application.dto.response.TicketAvailabilityResponse;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.repository.dto.MinPerformanceDateDto;
import com.beat.domain.schedule.service.ScheduleDomainService;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ConflictException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDomainService scheduleDomainService = new ScheduleDomainService();

	@Transactional(readOnly = true)
	public TicketAvailabilityResponse findTicketAvailability(Long scheduleId,
		TicketAvailabilityRequest ticketAvailabilityRequest) {
		validateRequest(scheduleId, ticketAvailabilityRequest);

		Schedule schedule = scheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND));

		int availableTicketCount = scheduleDomainService.getAvailableTicketCount(schedule);
		boolean isAvailable = scheduleDomainService.canPurchase(schedule,
			ticketAvailabilityRequest.purchaseTicketCount());

		if (!isAvailable) {
			throw new ConflictException(ScheduleErrorCode.INSUFFICIENT_TICKETS);
		}

		return TicketAvailabilityResponse.of(
			schedule.getId(),
			schedule.getScheduleNumber().getDisplayName(),
			schedule.getTotalTicketCount(),
			schedule.getSoldTicketCount(),
			availableTicketCount,
			ticketAvailabilityRequest.purchaseTicketCount(),
			isAvailable
		);
	}

	@Transactional(readOnly = true)
	public MinPerformanceDateResponse retrieveMinPerformanceDateByPerformanceIds(List<Long> performanceIds) {
		List<MinPerformanceDateDto> minPerformanceDateDtos
			= scheduleRepository.findMinPerformanceDateByPerformanceIds(performanceIds);

		Map<Long, LocalDateTime> performanceDateMap = minPerformanceDateDtos.stream()
			.collect(Collectors.toMap(
				MinPerformanceDateDto::getPerformanceId,
				MinPerformanceDateDto::getPerformanceDate
			));

		return MinPerformanceDateResponse.from(performanceDateMap);
	}

	private void validateRequest(Long scheduleId, TicketAvailabilityRequest ticketAvailabilityRequest) {
		if (ticketAvailabilityRequest.purchaseTicketCount() <= 0 || scheduleId <= 0) {
			throw new BadRequestException(ScheduleErrorCode.INVALID_DATA_FORMAT);
		}
	}
}
