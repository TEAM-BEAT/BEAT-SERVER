package com.beat.domain.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.beat.domain.exception.DomainException;
import com.beat.domain.performance.model.Genre;
import com.beat.domain.performance.model.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;

class PerformanceDomainInvariantTest {

	@Test
	void createRejectsNegativeTicketPrice() {
		DomainException exception = assertThrows(DomainException.class, () -> performanceWith(60, -1, 1));

		assertEquals(PerformanceErrorCode.NEGATIVE_TICKET_PRICE, exception.getErrorCode());
	}

	@Test
	void createRejectsNonPositiveRunningTime() {
		DomainException exception = assertThrows(DomainException.class, () -> performanceWith(0, 10000, 1));

		assertEquals(PerformanceErrorCode.NON_POSITIVE_RUNNING_TIME, exception.getErrorCode());
	}

	@Test
	void createRejectsNegativeTotalScheduleCount() {
		DomainException exception = assertThrows(DomainException.class, () -> performanceWith(60, 10000, -1));

		assertEquals(PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT, exception.getErrorCode());
	}

	@Test
	void updateRejectsNonPositiveRunningTime() {
		Performance performance = performanceWith(60, 10000, 1);

		DomainException exception = assertThrows(DomainException.class, () -> updatePerformanceWith(performance, 0, 1));

		assertEquals(PerformanceErrorCode.NON_POSITIVE_RUNNING_TIME, exception.getErrorCode());
	}

	@Test
	void updateRejectsNegativeTotalScheduleCount() {
		Performance performance = performanceWith(60, 10000, 1);

		DomainException exception = assertThrows(DomainException.class, () -> updatePerformanceWith(performance, 60, -1));

		assertEquals(PerformanceErrorCode.NEGATIVE_SCHEDULE_COUNT, exception.getErrorCode());
	}

	@Test
	void updateTicketPriceRejectsNegativeTicketPrice() {
		Performance performance = performanceWith(60, 10000, 1);

		DomainException exception = assertThrows(DomainException.class, () -> performance.updateTicketPrice(-1));

		assertEquals(PerformanceErrorCode.NEGATIVE_TICKET_PRICE, exception.getErrorCode());
	}

	@Test
	void updateTicketPriceRejectsChangeWhenActiveBookingExists() {
		Performance performance = performanceWith(60, 10000, 1);

		DomainException exception = assertThrows(DomainException.class,
			() -> performance.updateTicketPrice(12000, true));

		assertEquals(PerformanceErrorCode.PRICE_UPDATE_NOT_ALLOWED, exception.getErrorCode());
	}

	@Test
	void ensureDeletableRejectsActiveBooking() {
		Performance performance = performanceWith(60, 10000, 1);

		DomainException exception = assertThrows(DomainException.class,
			() -> performance.ensureDeletable(true));

		assertEquals(PerformanceErrorCode.DELETE_NOT_ALLOWED, exception.getErrorCode());
	}

	private Performance performanceWith(int runningTime, int ticketPrice, int totalScheduleCount) {
		return Performance.create(
			"title",
			Genre.BAND,
			RunningTime.of(runningTime),
			"description",
			"attention",
			null,
			"poster",
			"team",
			"venue",
			"road",
			"detail",
			"37.1",
			"127.1",
			"010-1234-5678",
			PerformancePeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
			TicketPrice.of(ticketPrice),
			totalScheduleCount,
			1L
		);
	}

	private Performance updatePerformanceWith(Performance performance, int runningTime, int totalScheduleCount) {
		return performance.update(
			"title",
			Genre.BAND,
			RunningTime.of(runningTime),
			"description",
			"attention",
			null,
			"poster",
			"team",
			"venue",
			"road",
			"detail",
			"37.1",
			"127.1",
			"010-1234-5678",
			PerformancePeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
			totalScheduleCount
		);
	}
}
