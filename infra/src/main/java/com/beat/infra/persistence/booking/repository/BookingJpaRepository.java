package com.beat.infra.persistence.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.domain.booking.domain.BookingStatus;
import com.beat.infra.persistence.booking.entity.BookingJpaEntity;

public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, Long> {

	@Query("SELECT b FROM Booking b " +
		"WHERE b.bookerName = :bookerName " +
		"AND b.bookerPhoneNumber = :bookerPhoneNumber " +
		"AND b.password = :password " +
		"AND b.birthDate = :birthDate")
	Optional<List<BookingJpaEntity>> findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
		@Param("bookerName") String bookerName,
		@Param("bookerPhoneNumber") String bookerPhoneNumber,
		@Param("password") String password,
		@Param("birthDate") String birthDate
	);

	Optional<BookingJpaEntity> findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
		String bookerName,
		String bookerPhoneNumber,
		String birthDate,
		String password
	);

	List<BookingJpaEntity> findByUserId(Long userId);

	List<BookingJpaEntity> findByBookingStatusAndCancellationDateBefore(
		BookingStatus bookingStatus,
		java.time.LocalDateTime cancellationDate
	);

	@Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.scheduleId IN :scheduleIds AND b.bookingStatus NOT IN :excludedStatuses")
	boolean existsActiveBookingByScheduleIds(
		@Param("scheduleIds") List<Long> scheduleIds,
		@Param("excludedStatuses") List<BookingStatus> excludedStatuses
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM Booking b WHERE b.scheduleId IN :scheduleIds AND b.bookingStatus IN :inactiveStatuses")
	int deleteInactiveBookingsByScheduleIds(
		@Param("scheduleIds") List<Long> scheduleIds,
		@Param("inactiveStatuses") List<BookingStatus> inactiveStatuses
	);
}
