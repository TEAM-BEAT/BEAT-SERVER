package com.beat.domain.schedule.application;

import com.beat.domain.schedule.application.dto.TicketAvailabilityRequest;
import com.beat.domain.schedule.application.dto.TicketAvailabilityResponse;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ConflictException;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.OptionalInt;

@Service
@RequiredArgsConstructor
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;

	public TicketAvailabilityResponse findTicketAvailability(Long scheduleId,
		TicketAvailabilityRequest ticketAvailabilityRequest) {
		validateRequest(scheduleId, ticketAvailabilityRequest);

		Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(
			() -> new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND)
		);

		int availableTicketCount = getAvailableTicketCount(schedule);
		boolean isAvailable = availableTicketCount >= ticketAvailabilityRequest.purchaseTicketCount();

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

	public int getAvailableTicketCount(Schedule schedule) {
		return schedule.getTotalTicketCount() - schedule.getSoldTicketCount();
	}

	public int calculateDueDate(Schedule schedule) {
		int dueDate = (int)ChronoUnit.DAYS.between(LocalDate.now(), schedule.getPerformanceDate().toLocalDate());
		return dueDate;
	}

	public int getMinDueDateForPerformance(Long performanceId) {
		List<Schedule> schedules = scheduleRepository.findByPerformanceId(performanceId);
		return getMinDueDate(schedules);
	}

	public int getMinDueDate(List<Schedule> schedules) {
		OptionalInt minPositiveDueDate = schedules.stream()
			.mapToInt(this::calculateDueDate)
			.filter(dueDate -> dueDate >= 0)
			.min();

		if (minPositiveDueDate.isPresent()) {
			return minPositiveDueDate.getAsInt();
		} else {
			return schedules.stream()
				.mapToInt(this::calculateDueDate)
				.min()
				.orElse(Integer.MAX_VALUE);
		}
	}

	private void validateRequest(Long scheduleId, TicketAvailabilityRequest ticketAvailabilityRequest) {
		if (ticketAvailabilityRequest.purchaseTicketCount() <= 0 || scheduleId <= 0) {
			throw new BadRequestException(ScheduleErrorCode.INVALID_DATA_FORMAT);
		}
	}
}
