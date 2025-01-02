package com.beat.domain.booking.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;

public interface TicketRepository extends JpaRepository<Booking, Long>, TicketRepositoryCustom {

	List<Booking> findByBookingStatusAndCancellationDateBefore(BookingStatus bookingStatus,
		LocalDateTime cancellationDate);

}
