package com.beat.domain.booking.dao;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.schedule.domain.ScheduleNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Booking, Long> {
    List<Booking> findBySchedulePerformanceId(Long performanceId);

    List<Booking> findBySchedulePerformanceIdAndScheduleScheduleNumber(Long performanceId, ScheduleNumber scheduleNumber);

    List<Booking> findBySchedulePerformanceIdAndIsPaymentCompleted(Long performanceId, boolean isPaymentCompleted);

    List<Booking> findBySchedulePerformanceIdAndScheduleScheduleNumberAndIsPaymentCompleted(Long performanceId, ScheduleNumber scheduleNumber, boolean isPaymentCompleted);
}
