package com.beat.domain.booking.dao;

import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.beat.domain.booking.domain.QBooking.booking;
import static com.beat.domain.schedule.domain.QSchedule.schedule;

@Repository
@RequiredArgsConstructor
public class TicketRepositoryCustomImpl implements TicketRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	/**
	 * performanceId, scheduleNumbers, bookingStatuses 조건 필터
	 */
	@Override
	public List<Booking> findBookingsByPerformanceIdAndScheduleNumbersAndBookingStatuses(
		Long performanceId,
		List<ScheduleNumber> scheduleNumbers,
		List<BookingStatus> bookingStatuses
	) {
		return queryFactory
			.selectFrom(booking)
			.join(booking.schedule, schedule).fetchJoin()
			.where(
				eqPerformanceId(performanceId),
				booking.bookingStatus.ne(BookingStatus.BOOKING_DELETED),
				inScheduleNumbers(scheduleNumbers),
				inBookingStatuses(bookingStatuses)
			)
			.orderBy(
				new CaseBuilder()
					.when(booking.bookingStatus.eq(BookingStatus.REFUND_REQUESTED)).then(1)
					.when(booking.bookingStatus.eq(BookingStatus.CHECKING_PAYMENT)).then(2)
					.when(booking.bookingStatus.eq(BookingStatus.BOOKING_CONFIRMED)).then(3)
					.when(booking.bookingStatus.eq(BookingStatus.BOOKING_CANCELLED)).then(4)
					.otherwise(5).asc(),
				booking.createdAt.desc()
			)
			.fetch();
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
		return queryFactory
			.selectFrom(booking)
			.join(booking.schedule, schedule).fetchJoin()
			.where(
				eqPerformanceId(performanceId),
				booking.bookingStatus.ne(BookingStatus.BOOKING_DELETED),
				inScheduleNumbersByString(selectedScheduleNumbers),
				inBookingStatusesByString(selectedBookingStatuses),
				matchBookerName(searchWord).gt(0) // FullText match 결과가 0보다 큰 row만
			)
			.orderBy(
				new CaseBuilder()
					.when(booking.bookingStatus.eq(BookingStatus.REFUND_REQUESTED)).then(1)
					.when(booking.bookingStatus.eq(BookingStatus.CHECKING_PAYMENT)).then(2)
					.when(booking.bookingStatus.eq(BookingStatus.BOOKING_CONFIRMED)).then(3)
					.when(booking.bookingStatus.eq(BookingStatus.BOOKING_CANCELLED)).then(4)
					.otherwise(5).asc(),
				booking.createdAt.desc()
			)
			.fetch();
	}

	/* ======================
	   BooleanExpression 메서드들
	   ====================== */
	private BooleanExpression eqPerformanceId(Long performanceId) {
		if (performanceId == null) return null;
		return schedule.performance.id.eq(performanceId);
	}

	private BooleanExpression inScheduleNumbers(List<ScheduleNumber> scheduleNumbers) {
		if (scheduleNumbers == null || scheduleNumbers.isEmpty()) return null;
		return schedule.scheduleNumber.in(scheduleNumbers);
	}

	private BooleanExpression inScheduleNumbersByString(List<String> scheduleNumbers) {
		if (scheduleNumbers == null || scheduleNumbers.isEmpty()) return null;
		return schedule.scheduleNumber.stringValue().in(scheduleNumbers);
	}

	private BooleanExpression inBookingStatuses(List<BookingStatus> bookingStatuses) {
		if (bookingStatuses == null || bookingStatuses.isEmpty()) return null;
		return booking.bookingStatus.in(bookingStatuses);
	}

	private BooleanExpression inBookingStatusesByString(List<String> bookingStatuses) {
		if (bookingStatuses == null || bookingStatuses.isEmpty()) return null;
		return booking.bookingStatus.stringValue().in(bookingStatuses);
	}

	/**
	 * Dialect에 등록한 match( col, keyword ) 함수를 이용한 검색
	 */
	private NumberTemplate<Double> matchBookerName(String searchWord) {
		if (searchWord == null || searchWord.isBlank()) {
			// null이나 공백이면 조건을 무시하기 위해 null 반환 -> .where(...)에서 skip
			// 또는 null 대신 0.0 반환도 가능
			return Expressions.numberTemplate(Double.class, "0");
		}
		return Expressions.numberTemplate(
			Double.class,
			"function('match', {0}, {1})",   // match( booking.bookerName, :searchWord )
			booking.bookerName,
			searchWord
		);
	}
}
