package com.beat.apis.schedule.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.schedule.application.dto.request.TicketAvailabilityRequest;
import com.beat.apis.schedule.application.dto.response.MinPerformanceDateResponse;
import com.beat.apis.schedule.application.dto.response.TicketAvailabilityResponse;
import com.beat.contracts.schedule.ScheduleReadPort;
import com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.service.ScheduleDomainService;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ConflictException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import com.beat.apis.schedule.application.exception.ScheduleApplicationErrorCode;

@Service
@RequiredArgsConstructor
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleReadPort scheduleReadPort;
	private final ScheduleDomainService scheduleDomainService = new ScheduleDomainService();

	@Transactional(readOnly = true)
	public TicketAvailabilityResponse findTicketAvailability(Long scheduleId,
		TicketAvailabilityRequest ticketAvailabilityRequest) {
		validateRequest(scheduleId, ticketAvailabilityRequest);

		Schedule schedule = scheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new NotFoundException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND));

		int availableTicketCount = scheduleDomainService.getAvailableTicketCount(schedule);
		boolean isAvailable = scheduleDomainService.canPurchase(schedule,
			ticketAvailabilityRequest.purchaseTicketCount());

		if (!isAvailable) {
			throw new ConflictException(ScheduleApplicationErrorCode.INSUFFICIENT_TICKETS);
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
		List<MinPerformanceDateReadModel> minPerformanceDates
			= scheduleReadPort.findMinPerformanceDateByPerformanceIds(performanceIds);

		Map<Long, LocalDateTime> performanceDateMap = minPerformanceDates.stream()
			.collect(Collectors.toMap(
				MinPerformanceDateReadModel::performanceId,
				MinPerformanceDateReadModel::performanceDate,
				(existing, ignored) -> existing
			));

		return MinPerformanceDateResponse.from(performanceDateMap);
	}

	private void validateRequest(Long scheduleId, TicketAvailabilityRequest ticketAvailabilityRequest) {
		if (ticketAvailabilityRequest.purchaseTicketCount() <= 0 || scheduleId <= 0) {
			throw new BadRequestException(ScheduleApplicationErrorCode.INVALID_DATA_FORMAT);
		}
	}
}
