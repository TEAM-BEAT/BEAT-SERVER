package com.beat.domain.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.global.common.exception.BadRequestException;

class PerformanceDomainInvariantTest {

	@Test
	void createRejectsNegativeTicketPrice() {
		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> performanceWith(60, -1, 1)
		);

		assertEquals(PerformanceErrorCode.NEGATIVE_TICKET_PRICE, exception.getBaseErrorCode());
	}

	@Test
	void createRejectsNonPositiveRunningTime() {
		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> performanceWith(0, 10000, 1)
		);

		assertEquals(PerformanceErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void createRejectsNegativeTotalScheduleCount() {
		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> performanceWith(60, 10000, -1)
		);

		assertEquals(PerformanceErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void updateRejectsNonPositiveRunningTime() {
		Performance performance = performanceWith(60, 10000, 1);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> updatePerformanceWith(performance, 0, 1)
		);

		assertEquals(PerformanceErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void updateRejectsNegativeTotalScheduleCount() {
		Performance performance = performanceWith(60, 10000, 1);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> updatePerformanceWith(performance, 60, -1)
		);

		assertEquals(PerformanceErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void updateTicketPriceRejectsNegativeTicketPrice() {
		Performance performance = performanceWith(60, 10000, 1);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> performance.updateTicketPrice(-1)
		);

		assertEquals(PerformanceErrorCode.NEGATIVE_TICKET_PRICE, exception.getBaseErrorCode());
	}

	private Performance performanceWith(int runningTime, int ticketPrice, int totalScheduleCount) {
		return Performance.create(
			"title",
			Genre.BAND,
			runningTime,
			"description",
			"attention",
			null,
			null,
			null,
			"poster",
			"team",
			"venue",
			"road",
			"detail",
			"37.1",
			"127.1",
			"010-1234-5678",
			"2026.01.01",
			ticketPrice,
			totalScheduleCount,
			1L
		);
	}

	private Performance updatePerformanceWith(Performance performance, int runningTime, int totalScheduleCount) {
		return performance.update(
			"title",
			Genre.BAND,
			runningTime,
			"description",
			"attention",
			null,
			null,
			null,
			"poster",
			"team",
			"venue",
			"road",
			"detail",
			"37.1",
			"127.1",
			"010-1234-5678",
			"2026.01.01",
			totalScheduleCount
		);
	}
}
