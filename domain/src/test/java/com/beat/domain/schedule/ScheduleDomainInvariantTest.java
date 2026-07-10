package com.beat.domain.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.global.support.exception.BadRequestException;
import com.beat.global.support.exception.ConflictException;

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
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);
		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> Schedule.create(performanceDate, performanceDate.plusHours(1), -1, ScheduleNumber.FIRST, 1L)
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
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> schedule.update(performanceDate, performanceDate.plusHours(1), 2, ScheduleNumber.SECOND)
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void updateRejectsNegativeTotalTicketCount() {
		Schedule schedule = scheduleWithSoldTicketCount(10, 3);
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> schedule.update(performanceDate, performanceDate.plusHours(1), -1, ScheduleNumber.SECOND)
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	@Test
	void updateBookingCloseAtAllowsPerformanceExtension() {
		Schedule schedule = scheduleWithSoldTicketCount(10, 3);
		LocalDateTime extendedCloseAt = schedule.getBookingCloseAt().plusHours(1);

		Schedule extendedSchedule = schedule.updateBookingCloseAt(extendedCloseAt);

		assertEquals(extendedCloseAt, extendedSchedule.getBookingCloseAt());
	}

	@Test
	void updateBookingCloseAtRejectsTimeBeforePerformanceStart() {
		Schedule schedule = scheduleWithSoldTicketCount(10, 3);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> schedule.updateBookingCloseAt(schedule.getPerformanceDate().minusNanos(1))
		);

		assertEquals(ScheduleErrorCode.INVALID_DATA_FORMAT, exception.getBaseErrorCode());
	}

	private Schedule scheduleWithSoldTicketCount(int totalTicketCount, int soldTicketCount) {
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);
		return Schedule.rehydrate(
			1L,
			performanceDate,
			performanceDate.plusHours(1),
			totalTicketCount,
			soldTicketCount,
			ScheduleNumber.FIRST,
			1L
		);
	}
}
