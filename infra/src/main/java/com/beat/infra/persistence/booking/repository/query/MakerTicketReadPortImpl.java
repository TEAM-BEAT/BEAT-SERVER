package com.beat.infra.persistence.booking.repository.query;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.beat.contracts.booking.MakerTicketReadPort;
import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.infra.persistence.booking.entity.BookingJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@Repository
public class MakerTicketReadPortImpl implements MakerTicketReadPort {

	private final EntityManager entityManager;

	public MakerTicketReadPortImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public List<MakerTicketListItemReadModel> findTickets(Long performanceId, List<String> scheduleNumberNames,
		List<String> bookingStatusNames) {
		List<ScheduleNumber> scheduleNumbers = toScheduleNumbers(scheduleNumberNames);
		List<BookingStatus> bookingStatuses = toBookingStatuses(bookingStatusNames);

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
		return toReadModels(query.getResultList());
	}

	@Override
	public List<MakerTicketListItemReadModel> searchTickets(Long performanceId, String searchWord,
		List<String> scheduleNumberNames, List<String> bookingStatusNames) {
		if (searchWord == null || searchWord.isBlank()) {
			return List.of();
		}

		List<ScheduleNumber> scheduleNumbers = toScheduleNumbers(scheduleNumberNames);
		List<BookingStatus> bookingStatuses = toBookingStatuses(bookingStatusNames);

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
		return toReadModels(query.getResultList());
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

	private List<MakerTicketListItemReadModel> toReadModels(List<BookingJpaEntity> entities) {
		return entities.stream()
			.map(this::toReadModel)
			.toList();
	}

	private MakerTicketListItemReadModel toReadModel(BookingJpaEntity entity) {
		return new MakerTicketListItemReadModel(
			entity.getId(),
			entity.getBookerName(),
			entity.getBookerPhoneNumber(),
			entity.getScheduleId(),
			entity.getPurchaseTicketCount(),
			entity.getCreatedAt(),
			entity.getBookingStatus().name(),
			toBankName(entity.getBankName()),
			nullToEmpty(entity.getAccountNumber()),
			nullToEmpty(entity.getAccountHolder())
		);
	}

	private String toBankName(BankName bankName) {
		return bankName == null ? BankName.NONE.getDisplayName() : bankName.getDisplayName();
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
