package com.beat.domain.booking.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.domain.booking.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
	@Query("SELECT b FROM Booking b " +
		"JOIN b.schedule s " +
		"JOIN s.performance p " +
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

	List<Booking> findByUsersId(Long userId);

	@Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.schedule.id IN :scheduleIds AND b.bookingStatus != 'BOOKING_CANCELLED'")
	boolean existsByScheduleIdIn(@Param("scheduleIds") List<Long> scheduleIds);
}