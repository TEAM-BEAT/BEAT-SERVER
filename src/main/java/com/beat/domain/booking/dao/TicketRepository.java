package com.beat.domain.booking.dao;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Booking, Long> {
    List<Booking> findBySchedulePerformanceId(Long performanceId);

    List<Booking> findBySchedulePerformanceIdAndScheduleScheduleNumber(Long performanceId, ScheduleNumber scheduleNumber);

    List<Booking> findBySchedulePerformanceIdAndBookingStatus(Long performanceId, BookingStatus bookingStatus);

    List<Booking> findBySchedulePerformanceIdAndScheduleScheduleNumberAndBookingStatus(Long performanceId, ScheduleNumber scheduleNumber, BookingStatus bookingStatus);

    List<Booking> findByBookingStatusAndCancellationDateBefore(BookingStatus bookingStatus, LocalDateTime cancellationDate);
}
