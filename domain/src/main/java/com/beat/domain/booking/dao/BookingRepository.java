package com.beat.domain.booking.dao;

import java.util.List;
import java.util.Optional;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;

public interface BookingRepository {

	Booking save(Booking booking);

	Optional<Booking> findById(Long id);

	List<Booking> findAll();

	Optional<List<Booking>> findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
		String bookerName,
		String bookerPhoneNumber,
		String password,
		String birthDate
	);

	Optional<Booking> findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
		String bookerName,
		String bookerPhoneNumber,
		String birthDate,
		String password
	);

	List<Booking> findByUserId(Long userId);

	boolean existsActiveBookingByScheduleIds(
		List<Long> scheduleIds,
		List<BookingStatus> excludedStatuses
	);

	int deleteInactiveBookingsByScheduleIds(
		List<Long> scheduleIds,
		List<BookingStatus> inactiveStatuses
	);
}
