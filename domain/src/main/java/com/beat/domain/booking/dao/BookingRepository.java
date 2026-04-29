package com.beat.domain.booking.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {
	@Query("SELECT b FROM Booking b " +
		"WHERE b.bookerName = :bookerName " +
		"AND b.bookerPhoneNumber = :bookerPhoneNumber " +
		"AND b.password = :password " +
		"AND b.birthDate = :birthDate")
	Optional<List<Booking>> findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
		@Param("bookerName") String bookerName,
		@Param("bookerPhoneNumber") String bookerPhoneNumber,
		@Param("password") String password,
		@Param("birthDate") String birthDate
	);

	Optional<Booking> findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
		String bookerName,
		String bookerPhoneNumber,
		String birthDate,
		String password
	);

	List<Booking> findByUserId(Long userId);

	@Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.scheduleId IN :scheduleIds AND b.bookingStatus NOT IN :excludedStatuses")
	boolean existsActiveBookingByScheduleIds(
		@Param("scheduleIds") List<Long> scheduleIds,
		@Param("excludedStatuses") List<BookingStatus> excludedStatuses
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM Booking b WHERE b.scheduleId IN :scheduleIds AND b.bookingStatus IN :inactiveStatuses")
	void deleteInactiveBookingsByScheduleIds(
		@Param("scheduleIds") List<Long> scheduleIds,
		@Param("inactiveStatuses") List<BookingStatus> inactiveStatuses
	);
}
