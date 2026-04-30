package com.beat.infra.persistence.booking.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.beat.domain.booking.dao.TicketRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.infra.persistence.booking.entity.BookingJpaEntity;
import com.beat.infra.persistence.booking.mapper.BookingPersistenceMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@Repository
public class TicketRepositoryImpl implements TicketRepository {

	private final BookingJpaRepository bookingJpaRepository;
	private final BookingPersistenceMapper bookingPersistenceMapper;
	private final EntityManager entityManager;

	public TicketRepositoryImpl(BookingJpaRepository bookingJpaRepository,
		BookingPersistenceMapper bookingPersistenceMapper,
		EntityManager entityManager) {
		this.bookingJpaRepository = bookingJpaRepository;
		this.bookingPersistenceMapper = bookingPersistenceMapper;
		this.entityManager = entityManager;
	}

	@Override
	public Optional<Booking> findById(Long id) {
		return bookingJpaRepository.findById(id).map(bookingPersistenceMapper::toDomain);
	}

	@Override
	public Booking save(Booking booking) {
		BookingJpaEntity savedEntity = bookingJpaRepository.save(bookingPersistenceMapper.toEntity(booking));
		return bookingPersistenceMapper.toDomain(savedEntity);
	}

	@Override
	public void deleteAll(Iterable<Booking> bookings) {
		List<BookingJpaEntity> entities = toEntityList(bookings);
		bookingJpaRepository.deleteAll(entities);
	}

	@Override
	public List<Booking> findByBookingStatusAndCancellationDateBefore(BookingStatus bookingStatus,
		LocalDateTime cancellationDate) {
		return bookingJpaRepository.findByBookingStatusAndCancellationDateBefore(bookingStatus, cancellationDate).stream()
			.map(bookingPersistenceMapper::toDomain)
			.toList();
	}

	/**
	 * performanceId, scheduleNumbers, bookingStatuses 조건 필터
	 */
	@Override
	public List<Booking> findBookingsByPerformanceIdAndScheduleNumbersAndBookingStatuses(
		Long performanceId,
		List<ScheduleNumber> scheduleNumbers,
		List<BookingStatus> bookingStatuses
	) {
		StringBuilder jpql = baseBookingScheduleJpql();
		appendPerformanceCondition(jpql, performanceId);
		appendScheduleNumberCondition(jpql, scheduleNumbers);
		appendBookingStatusCondition(jpql, bookingStatuses);
		jpql.append(orderByBookingStatusAndCreatedAt());

		TypedQuery<BookingJpaEntity> query = entityManager.createQuery(jpql.toString(), BookingJpaEntity.class);
		bindCommonParameters(query);
		bindPerformanceParameter(query, performanceId);
		bindScheduleNumberParameter(query, scheduleNumbers);
		bindBookingStatusParameter(query, bookingStatuses);
		return toDomainList(query.getResultList());
	}

	@Override
	public List<Booking> searchBookingsByPerformanceIdAndSearchWordAndSchedulesNumbersAndBookingStatuses(
		Long performanceId,
		String searchWord,
		List<String> selectedScheduleNumbers,
		List<String> selectedBookingStatuses
	) {
		if (searchWord == null || searchWord.isBlank()) {
			return List.of();
		}

		List<ScheduleNumber> scheduleNumbers = toScheduleNumbers(selectedScheduleNumbers);
		List<BookingStatus> bookingStatuses = toBookingStatuses(selectedBookingStatuses);

		StringBuilder jpql = baseBookingScheduleJpql();
		appendPerformanceCondition(jpql, performanceId);
		appendScheduleNumberCondition(jpql, scheduleNumbers);
		appendBookingStatusCondition(jpql, bookingStatuses);
		jpql.append("  AND function('match', b.bookerName, :searchWord) > 0\n");
		jpql.append(orderByBookingStatusAndCreatedAt());

		TypedQuery<BookingJpaEntity> query = entityManager.createQuery(jpql.toString(), BookingJpaEntity.class);
		bindCommonParameters(query);
		bindPerformanceParameter(query, performanceId);
		bindScheduleNumberParameter(query, scheduleNumbers);
		bindBookingStatusParameter(query, bookingStatuses);
		query.setParameter("searchWord", searchWord);
		return toDomainList(query.getResultList());
	}

	private StringBuilder baseBookingScheduleJpql() {
		return new StringBuilder("""
			SELECT b
			FROM Booking b, Schedule s
			WHERE s.id = b.scheduleId
			  AND b.bookingStatus <> :deletedStatus
			""");
	}

	private void appendPerformanceCondition(StringBuilder jpql, Long performanceId) {
		if (performanceId != null) {
			jpql.append("  AND s.performanceId = :performanceId\n");
		}
	}

	private void appendScheduleNumberCondition(StringBuilder jpql, List<ScheduleNumber> scheduleNumbers) {
		if (scheduleNumbers != null && !scheduleNumbers.isEmpty()) {
			jpql.append("  AND s.scheduleNumber IN :scheduleNumbers\n");
		}
	}

	private void appendBookingStatusCondition(StringBuilder jpql, List<BookingStatus> bookingStatuses) {
		if (bookingStatuses != null && !bookingStatuses.isEmpty()) {
			jpql.append("  AND b.bookingStatus IN :bookingStatuses\n");
		}
	}

	private String orderByBookingStatusAndCreatedAt() {
		return """
			ORDER BY
			  CASE
			    WHEN b.bookingStatus = :refundRequestedStatus THEN 1
			    WHEN b.bookingStatus = :checkingPaymentStatus THEN 2
			    WHEN b.bookingStatus = :bookingConfirmedStatus THEN 3
			    WHEN b.bookingStatus = :bookingCancelledStatus THEN 4
			    ELSE 5
			  END ASC,
			  b.createdAt DESC
			""";
	}

	private void bindCommonParameters(TypedQuery<BookingJpaEntity> query) {
		query.setParameter("deletedStatus", BookingStatus.BOOKING_DELETED);
		query.setParameter("refundRequestedStatus", BookingStatus.REFUND_REQUESTED);
		query.setParameter("checkingPaymentStatus", BookingStatus.CHECKING_PAYMENT);
		query.setParameter("bookingConfirmedStatus", BookingStatus.BOOKING_CONFIRMED);
		query.setParameter("bookingCancelledStatus", BookingStatus.BOOKING_CANCELLED);
	}

	private void bindPerformanceParameter(TypedQuery<BookingJpaEntity> query, Long performanceId) {
		if (performanceId != null) {
			query.setParameter("performanceId", performanceId);
		}
	}

	private void bindScheduleNumberParameter(TypedQuery<BookingJpaEntity> query, List<ScheduleNumber> scheduleNumbers) {
		if (scheduleNumbers != null && !scheduleNumbers.isEmpty()) {
			query.setParameter("scheduleNumbers", scheduleNumbers);
		}
	}

	private void bindBookingStatusParameter(TypedQuery<BookingJpaEntity> query, List<BookingStatus> bookingStatuses) {
		if (bookingStatuses != null && !bookingStatuses.isEmpty()) {
			query.setParameter("bookingStatuses", bookingStatuses);
		}
	}

	private List<ScheduleNumber> toScheduleNumbers(List<String> scheduleNumbers) {
		if (scheduleNumbers == null || scheduleNumbers.isEmpty()) {
			return List.of();
		}
		return scheduleNumbers.stream()
			.map(ScheduleNumber::valueOf)
			.toList();
	}

	private List<BookingStatus> toBookingStatuses(List<String> bookingStatuses) {
		if (bookingStatuses == null || bookingStatuses.isEmpty()) {
			return List.of();
		}
		return bookingStatuses.stream()
			.map(BookingStatus::valueOf)
			.toList();
	}

	private List<Booking> toDomainList(List<BookingJpaEntity> entities) {
		return entities.stream()
			.map(bookingPersistenceMapper::toDomain)
			.toList();
	}

	private List<BookingJpaEntity> toEntityList(Iterable<Booking> bookings) {
		return org.springframework.data.util.Streamable.of(bookings).stream()
			.map(bookingPersistenceMapper::toEntity)
			.toList();
	}
}
