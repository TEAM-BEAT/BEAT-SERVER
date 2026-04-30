package com.beat.domain.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.service.ScheduleDomainService;

class ScheduleDomainServiceTest {

	private final ScheduleDomainService scheduleDomainService = new ScheduleDomainService();
	private final LocalDate today = LocalDate.of(2026, 4, 30);

	@Test
	void calculateDueDateUsesProvidedToday() {
		Schedule schedule = scheduleOn(today.plusDays(3));

		int dueDate = scheduleDomainService.calculateDueDate(today, schedule);

		assertEquals(3, dueDate);
	}

	@Test
	void calculateDueDateReturnsZeroForSameDaySchedule() {
		Schedule schedule = scheduleOn(today);

		int dueDate = scheduleDomainService.calculateDueDate(today, schedule);

		assertEquals(0, dueDate);
	}

	@Test
	void calculateDueDateCanReturnNegativeValueForPastSchedule() {
		Schedule schedule = scheduleOn(today.minusDays(2));

		int dueDate = scheduleDomainService.calculateDueDate(today, schedule);

		assertEquals(-2, dueDate);
	}

	@Test
	void getMinDueDatePrefersNearestNonPastSchedule() {
		List<Schedule> schedules = List.of(
			scheduleOn(today.plusDays(7)),
			scheduleOn(today.minusDays(1)),
			scheduleOn(today.plusDays(2))
		);

		int minDueDate = scheduleDomainService.getMinDueDate(today, schedules);

		assertEquals(2, minDueDate);
	}

	@Test
	void getMinDueDatePrefersSameDayScheduleOverFutureSchedule() {
		List<Schedule> schedules = List.of(
			scheduleOn(today.plusDays(1)),
			scheduleOn(today),
			scheduleOn(today.plusDays(2))
		);

		int minDueDate = scheduleDomainService.getMinDueDate(today, schedules);

		assertEquals(0, minDueDate);
	}

	@Test
	void getMinDueDateFallsBackToEarliestPastDueDateWhenNoFutureScheduleExists() {
		List<Schedule> schedules = List.of(
			scheduleOn(today.minusDays(1)),
			scheduleOn(today.minusDays(4))
		);

		int minDueDate = scheduleDomainService.getMinDueDate(today, schedules);

		assertEquals(-4, minDueDate);
	}

	@Test
	void getMinDueDateReturnsMaxValueWhenSchedulesAreEmpty() {
		int minDueDate = scheduleDomainService.getMinDueDate(today, List.of());

		assertEquals(Integer.MAX_VALUE, minDueDate);
	}

	@Test
	void getAvailableTicketCountSubtractsSoldTicketsFromTotalTickets() {
		Schedule schedule = scheduleWithTicketCounts(10, 3);

		int availableTicketCount = scheduleDomainService.getAvailableTicketCount(schedule);

		assertEquals(7, availableTicketCount);
	}

	@Test
	void canPurchaseReturnsTrueWhenAvailableTicketCountCoversRequest() {
		Schedule schedule = scheduleWithTicketCounts(10, 3);

		boolean canPurchase = scheduleDomainService.canPurchase(schedule, 7);

		assertTrue(canPurchase);
	}

	@Test
	void canPurchaseReturnsFalseWhenRequestExceedsAvailableTicketCount() {
		Schedule schedule = scheduleWithTicketCounts(10, 3);

		boolean canPurchase = scheduleDomainService.canPurchase(schedule, 8);

		assertFalse(canPurchase);
	}

	private Schedule scheduleOn(LocalDate performanceDate) {
		return Schedule.rehydrate(
			1L,
			LocalDateTime.of(performanceDate, LocalTime.NOON),
			10,
			0,
			true,
			ScheduleNumber.FIRST,
			1L
		);
	}

	private Schedule scheduleWithTicketCounts(int totalTicketCount, int soldTicketCount) {
		return Schedule.rehydrate(
			1L,
			LocalDateTime.of(2026, 5, 1, 12, 0),
			totalTicketCount,
			soldTicketCount,
			true,
			ScheduleNumber.FIRST,
			1L
		);
	}
}
