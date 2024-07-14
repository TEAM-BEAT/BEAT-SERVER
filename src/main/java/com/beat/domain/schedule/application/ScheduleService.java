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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public TicketAvailabilityResponse findTicketAvailability(Long scheduleId, TicketAvailabilityRequest ticketAvailabilityRequest) {
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

    private void validateRequest(Long scheduleId, TicketAvailabilityRequest ticketAvailabilityRequest) {
        if (ticketAvailabilityRequest.purchaseTicketCount() <= 0 || scheduleId <= 0) {
            throw new BadRequestException(ScheduleErrorCode.INVALID_DATA_FORMAT);
        }
    }

    public int getAvailableTicketCount(Schedule schedule) {
        return schedule.getTotalTicketCount() - schedule.getSoldTicketCount();
    }

    public boolean isBookingAvailable(Schedule schedule) {
        int availableTicketCount = getAvailableTicketCount(schedule);
        return schedule.isBooking() && availableTicketCount > 0 && schedule.getPerformanceDate().isAfter(LocalDateTime.now());
    }

    @Transactional
    public void updateBookingStatus(Schedule schedule) {
        boolean isBookingAvailable = isBookingAvailable(schedule);
        if (schedule.isBooking() != isBookingAvailable) {
            schedule.setBooking(isBookingAvailable);
            scheduleRepository.save(schedule);
        }
    }
}