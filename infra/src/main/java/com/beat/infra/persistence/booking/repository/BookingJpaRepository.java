package com.beat.infra.persistence.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beat.domain.booking.model.BookingStatus;
import com.beat.infra.persistence.booking.entity.BookingJpaEntity;

import jakarta.persistence.LockModeType;

public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT b FROM Booking b WHERE b.id = :id")
	Optional<BookingJpaEntity> lockById(@Param("id") Long id);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Booking b SET b.password = :encodedPassword WHERE b.userId = :userId AND b.birthDate IS NOT NULL")
	int replaceGuestPassword(
		@Param("userId") Long userId,
		@Param("encodedPassword") String encodedPassword
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
