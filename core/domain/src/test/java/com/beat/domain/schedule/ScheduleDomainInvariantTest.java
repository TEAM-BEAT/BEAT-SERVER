package com.beat.domain.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.beat.domain.schedule.model.Schedule;
import com.beat.domain.schedule.model.ScheduleNumber;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.exception.DomainException;

class ScheduleDomainInvariantTest {
	@Test
	void reserveAndReleaseTicketsPreserveAllocationInvariant() {
		Schedule schedule = scheduleWithAllocatedTicketCount(10, 3);

		Schedule reserved = schedule.reserveTickets(2);
		Schedule released = reserved.releaseTickets(1);

		assertEquals(3, schedule.getAllocatedTicketCount());
		assertEquals(5, reserved.getAllocatedTicketCount());
		assertEquals(4, released.getAllocatedTicketCount());
		assertEquals(6, released.getAvailableTicketCount());
	}

	@Test
	void reserveTicketsRejectsNonPositiveCount() {
		Schedule schedule = scheduleWithAllocatedTicketCount(10, 0);

		DomainException exception = assertThrows(DomainException.class, () -> schedule.reserveTickets(0));

		assertEquals(ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT, exception.getErrorCode());
	}

	@Test
	void reserveTicketsRejectsTotalTicketOverflow() {
		Schedule schedule = scheduleWithAllocatedTicketCount(5, 4);

		DomainException exception = assertThrows(DomainException.class, () -> schedule.reserveTickets(2));

		assertEquals(ScheduleErrorCode.INSUFFICIENT_TICKETS, exception.getErrorCode());
	}

	@Test
	void releaseTicketsRejectsNonPositiveCount() {
		Schedule schedule = scheduleWithAllocatedTicketCount(10, 3);

		DomainException exception = assertThrows(DomainException.class, () -> schedule.releaseTickets(0));

		assertEquals(ScheduleErrorCode.NON_POSITIVE_TICKET_COUNT, exception.getErrorCode());
	}

	@Test
	void releaseTicketsRejectsUnderflow() {
		Schedule schedule = scheduleWithAllocatedTicketCount(10, 3);

		DomainException exception = assertThrows(DomainException.class, () -> schedule.releaseTickets(4));

		assertEquals(ScheduleErrorCode.EXCESS_TICKET_DELETE, exception.getErrorCode());
	}

	@Test
	void createRejectsNegativeTotalTicketCount() {
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);
		DomainException exception = assertThrows(DomainException.class, () -> Schedule.create(performanceDate, performanceDate.plusHours(1), -1, ScheduleNumber.FIRST, 1L));

		assertEquals(ScheduleErrorCode.NEGATIVE_TICKET_COUNT, exception.getErrorCode());
	}

	@Test
	void rehydrateRejectsNegativeAllocatedTicketCount() {
		DomainException exception = assertThrows(DomainException.class, () -> scheduleWithAllocatedTicketCount(10, -1));

		assertEquals(ScheduleErrorCode.NEGATIVE_TICKET_COUNT, exception.getErrorCode());
	}

	@Test
	void rehydrateRejectsAllocatedTicketCountAboveTotalTicketCount() {
		DomainException exception = assertThrows(DomainException.class, () -> scheduleWithAllocatedTicketCount(3, 4));

		assertEquals(ScheduleErrorCode.ALLOCATED_TICKETS_EXCEED_TOTAL, exception.getErrorCode());
	}

	@Test
	void updateRejectsTotalTicketCountBelowAllocatedTicketCount() {
		Schedule schedule = scheduleWithAllocatedTicketCount(10, 3);
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);

		DomainException exception = assertThrows(DomainException.class, () -> schedule.update(performanceDate, performanceDate.plusHours(1), 2, ScheduleNumber.SECOND));

		assertEquals(ScheduleErrorCode.ALLOCATED_TICKETS_EXCEED_TOTAL, exception.getErrorCode());
	}

	@Test
	void updateRejectsNegativeTotalTicketCount() {
		Schedule schedule = scheduleWithAllocatedTicketCount(10, 3);
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);

		DomainException exception = assertThrows(DomainException.class, () -> schedule.update(performanceDate, performanceDate.plusHours(1), -1, ScheduleNumber.SECOND));

		assertEquals(ScheduleErrorCode.NEGATIVE_TICKET_COUNT, exception.getErrorCode());
	}

	@Test
	void updateBookingCloseAtAllowsPerformanceExtension() {
		Schedule schedule = scheduleWithAllocatedTicketCount(10, 3);
		LocalDateTime extendedCloseAt = schedule.getBookingCloseAt().plusHours(1);

		Schedule extendedSchedule = schedule.updateBookingCloseAt(extendedCloseAt);

		assertEquals(extendedCloseAt, extendedSchedule.getBookingCloseAt());
	}

	@Test
	void updateBookingCloseAtRejectsTimeBeforePerformanceStart() {
		Schedule schedule = scheduleWithAllocatedTicketCount(10, 3);

		DomainException exception = assertThrows(DomainException.class, () -> schedule.updateBookingCloseAt(schedule.getPerformanceDate().minusNanos(1)));

		assertEquals(ScheduleErrorCode.INVALID_BOOKING_WINDOW, exception.getErrorCode());
	}

	@Test
	void createUpcomingRejectsPastPerformanceDate() {
		LocalDateTime now = LocalDateTime.of(2026, 1, 2, 12, 0);

		DomainException exception = assertThrows(DomainException.class, () -> Schedule.createUpcoming(
			now.minusMinutes(1), now.plusHours(1), 10, ScheduleNumber.FIRST, 1L, now));

		assertEquals(ScheduleErrorCode.PAST_SCHEDULE_NOT_ALLOWED, exception.getErrorCode());
	}

	@Test
	void rescheduleRejectsModifyingEndedSchedule() {
		LocalDateTime performanceDate = LocalDateTime.of(2026, 1, 1, 12, 0);
		Schedule schedule = Schedule.rehydrate(1L, performanceDate, performanceDate.plusHours(1), 10, 0,
			ScheduleNumber.FIRST, 7L);

		DomainException exception = assertThrows(DomainException.class, () -> schedule.reschedule(
			performanceDate, performanceDate.plusHours(1), 10, ScheduleNumber.FIRST,
			performanceDate.plusHours(2)));

		assertEquals(ScheduleErrorCode.ENDED_SCHEDULE_MODIFICATION_NOT_ALLOWED, exception.getErrorCode());
	}

	@Test
	void belongsToUsesPerformanceIdentity() {
		Schedule schedule = scheduleWithAllocatedTicketCount(10, 0);

		assertEquals(true, schedule.belongsTo(1L));
		assertEquals(false, schedule.belongsTo(2L));
	}

	private Schedule scheduleWithAllocatedTicketCount(int totalTicketCount, int allocatedTicketCount) {
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);
		return Schedule.rehydrate(
			1L,
			performanceDate,
			performanceDate.plusHours(1),
			totalTicketCount,
			allocatedTicketCount,
			ScheduleNumber.FIRST,
			1L
		);
	}
}
