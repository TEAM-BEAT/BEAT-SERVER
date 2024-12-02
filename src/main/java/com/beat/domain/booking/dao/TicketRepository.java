package com.beat.domain.booking.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;

public interface TicketRepository extends JpaRepository<Booking, Long> {
	@Query("SELECT b FROM Booking b " +
		"WHERE b.schedule.performance.id = :performanceId " +
		"AND b.bookingStatus != com.beat.domain.booking.domain.BookingStatus.BOOKING_DELETED " +
		"AND (:scheduleNumbers IS NULL OR b.schedule.scheduleNumber IN :scheduleNumbers) " +
		"AND (:bookingStatuses IS NULL OR b.bookingStatus IN :bookingStatuses) " +
		"ORDER BY CASE b.bookingStatus " +
		"           WHEN com.beat.domain.booking.domain.BookingStatus.REFUND_REQUESTED THEN 1 " +
		"           WHEN com.beat.domain.booking.domain.BookingStatus.CHECKING_PAYMENT THEN 2 " +
		"           WHEN com.beat.domain.booking.domain.BookingStatus.BOOKING_CONFIRMED THEN 3 " +
		"           WHEN com.beat.domain.booking.domain.BookingStatus.BOOKING_CANCELLED THEN 4 " +
		"         END ASC, " +
		"         b.createdAt DESC")
	List<Booking> findBookings(
		@Param("performanceId") Long performanceId,
		@Param("scheduleNumbers") List<ScheduleNumber> scheduleNumbers,
		@Param("bookingStatuses") List<BookingStatus> bookingStatuses);

	List<Booking> findByBookingStatusAndCancellationDateBefore(BookingStatus bookingStatus,
		LocalDateTime cancellationDate);

	@Query(value = """
		SELECT b.*
		FROM booking b
		JOIN schedule s ON b.schedule_id = s.id
		WHERE s.performance_id = :performanceId
		    AND b.booking_status != 'BOOKING_DELETED'
		    AND (s.schedule_number IN (:scheduleNumberStrings))
		    AND (b.booking_status IN (:bookingStatusStrings))
		    AND MATCH(b.booker_name) AGAINST(:searchWord IN BOOLEAN MODE)
		ORDER BY
		    CASE b.booking_status
		    WHEN 'REFUND_REQUESTED' THEN 1
		    WHEN 'CHECKING_PAYMENT' THEN 2
		    WHEN 'BOOKING_CONFIRMED' THEN 3
		    WHEN 'BOOKING_CANCELLED' THEN 4
		    END ASC,
		    b.created_at DESC
		""", nativeQuery = true)
	List<Booking> searchBookings(
		@Param("performanceId") Long performanceId,
		@Param("searchWord") String searchWord,
		@Param("scheduleNumberStrings") List<String> scheduleNumberStrings,
		@Param("bookingStatusStrings") List<String> bookingStatusStrings
	);

}
