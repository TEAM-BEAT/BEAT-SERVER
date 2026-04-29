package com.beat.domain.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ConflictException;

class ScheduleDomainInvariantTest {

	@Test
	void increaseSoldTicketCountRejectsNonPositiveCount() {
		Schedule schedule = scheduleWithSoldTicketCount(10, 0);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> schedule.increaseSoldTicketCount(0)
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void increaseSoldTicketCountRejectsTotalTicketOverflow() {
		Schedule schedule = scheduleWithSoldTicketCount(5, 4);

		ConflictException exception = assertThrows(
			ConflictException.class,
			() -> schedule.increaseSoldTicketCount(2)
		);

		assertEquals(ScheduleErrorCode.INSUFFICIENT_TICKETS, exception.getBaseErrorCode());
	}

	@Test
	void decreaseSoldTicketCountRejectsNonPositiveCount() {
		Schedule schedule = scheduleWithSoldTicketCount(10, 3);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> schedule.decreaseSoldTicketCount(0)
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void decreaseSoldTicketCountRejectsUnderflow() {
		Schedule schedule = scheduleWithSoldTicketCount(10, 3);

		ConflictException exception = assertThrows(
			ConflictException.class,
			() -> schedule.decreaseSoldTicketCount(4)
		);

		assertEquals(ScheduleErrorCode.EXCESS_TICKET_DELETE, exception.getBaseErrorCode());
	}

	@Test
	void createRejectsNegativeTotalTicketCount() {
		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> Schedule.create(LocalDateTime.now().plusDays(1), -1, ScheduleNumber.FIRST, 1L)
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void rehydrateRejectsNegativeSoldTicketCount() {
		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> scheduleWithSoldTicketCount(10, -1)
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void rehydrateRejectsSoldTicketCountAboveTotalTicketCount() {
		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> scheduleWithSoldTicketCount(3, 4)
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void updateRejectsTotalTicketCountBelowSoldTicketCount() {
		Schedule schedule = scheduleWithSoldTicketCount(10, 3);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> schedule.update(LocalDateTime.now().plusDays(1), 2, ScheduleNumber.SECOND)
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void updateRejectsNegativeTotalTicketCount() {
		Schedule schedule = scheduleWithSoldTicketCount(10, 3);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> schedule.update(LocalDateTime.now().plusDays(1), -1, ScheduleNumber.SECOND)
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	private Schedule scheduleWithSoldTicketCount(int totalTicketCount, int soldTicketCount) {
		return Schedule.rehydrate(
			1L,
			LocalDateTime.now().plusDays(1),
			totalTicketCount,
			soldTicketCount,
			true,
			ScheduleNumber.FIRST,
			1L
		);
	}
}
