package com.beat.domain.booking.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TicketRepositoryCustomImpl implements TicketRepositoryCustom {

	private final EntityManager entityManager;

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

		TypedQuery<Booking> query = entityManager.createQuery(jpql.toString(), Booking.class);
		bindCommonParameters(query);
		bindPerformanceParameter(query, performanceId);
		bindScheduleNumberParameter(query, scheduleNumbers);
		bindBookingStatusParameter(query, bookingStatuses);
		return query.getResultList();
	}

	/**
	 * MySQL ngram full text 검색용 쿼리
	 * - match(booking.bookerName, :searchWord) > 0
	 */
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

		TypedQuery<Booking> query = entityManager.createQuery(jpql.toString(), Booking.class);
		bindCommonParameters(query);
		bindPerformanceParameter(query, performanceId);
		bindScheduleNumberParameter(query, scheduleNumbers);
		bindBookingStatusParameter(query, bookingStatuses);
		query.setParameter("searchWord", searchWord);
		return query.getResultList();
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

	private void bindCommonParameters(TypedQuery<Booking> query) {
		query.setParameter("deletedStatus", BookingStatus.BOOKING_DELETED);
		query.setParameter("refundRequestedStatus", BookingStatus.REFUND_REQUESTED);
		query.setParameter("checkingPaymentStatus", BookingStatus.CHECKING_PAYMENT);
		query.setParameter("bookingConfirmedStatus", BookingStatus.BOOKING_CONFIRMED);
		query.setParameter("bookingCancelledStatus", BookingStatus.BOOKING_CANCELLED);
	}

	private void bindPerformanceParameter(TypedQuery<Booking> query, Long performanceId) {
		if (performanceId != null) {
			query.setParameter("performanceId", performanceId);
		}
	}

	private void bindScheduleNumberParameter(TypedQuery<Booking> query, List<ScheduleNumber> scheduleNumbers) {
		if (scheduleNumbers != null && !scheduleNumbers.isEmpty()) {
			query.setParameter("scheduleNumbers", scheduleNumbers);
		}
	}

	private void bindBookingStatusParameter(TypedQuery<Booking> query, List<BookingStatus> bookingStatuses) {
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
}
