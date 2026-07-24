package com.beat.domain.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.domain.exception.DomainException;
import com.beat.domain.schedule.model.Schedule;
import com.beat.domain.schedule.model.ScheduleNumber;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.schedule.service.ScheduleSequenceDomainService;

class ScheduleDomainServiceTest {

	private final ScheduleSequenceDomainService scheduleSequenceDomainService = new ScheduleSequenceDomainService();
	private final LocalDate today = LocalDate.of(2026, 4, 30);

	@Test
	void assignsScheduleNumbersChronologicallyWithoutMutatingInput() {
		Schedule laterSchedule = schedule(2L, today.plusDays(1));
		Schedule earlierSchedule = schedule(1L, today);
		List<Schedule> input = List.of(laterSchedule, earlierSchedule);

		List<Schedule> assigned = scheduleSequenceDomainService.assignScheduleNumbers(input);

		assertEquals(1L, assigned.get(0).getId());
		assertEquals(ScheduleNumber.FIRST, assigned.get(0).getScheduleNumber());
		assertEquals(2L, assigned.get(1).getId());
		assertEquals(ScheduleNumber.SECOND, assigned.get(1).getScheduleNumber());
		assertEquals(List.of(laterSchedule, earlierSchedule), input);
	}

	@Test
	void rejectsMoreSchedulesThanSupportedNumbers() {
		List<Schedule> schedules = new ArrayList<>();
		for (int index = 0; index <= ScheduleNumber.values().length; index++) {
			schedules.add(schedule((long)index, today.plusDays(index)));
		}

		DomainException exception = assertThrows(DomainException.class,
			() -> scheduleSequenceDomainService.assignScheduleNumbers(schedules));
		assertEquals(ScheduleErrorCode.TOO_MANY_SCHEDULES, exception.getErrorCode());
		scheduleSequenceDomainService.validateScheduleCount(ScheduleNumber.values().length);
	}

	@Test
	void rejectsSchedulesFromDifferentPerformances() {
		List<Schedule> schedules = List.of(
			schedule(1L, today, 10L),
			schedule(2L, today.plusDays(1), 20L)
		);

		DomainException exception = assertThrows(DomainException.class,
			() -> scheduleSequenceDomainService.assignScheduleNumbers(schedules));

		assertEquals(ScheduleErrorCode.MIXED_PERFORMANCE_SCHEDULES, exception.getErrorCode());
	}

	@Test
	void getAvailableTicketCountSubtractsSoldTicketsFromTotalTickets() {
		Schedule schedule = scheduleWithTicketCounts(10, 3);

		int availableTicketCount = schedule.getAvailableTicketCount();

		assertEquals(7, availableTicketCount);
	}

	@Test
	void canPurchaseReturnsTrueWhenAvailableTicketCountCoversRequest() {
		Schedule schedule = scheduleWithTicketCounts(10, 3);

		boolean canPurchase = schedule.canPurchase(7);

		assertTrue(canPurchase);
	}

	@Test
	void canPurchaseReturnsFalseWhenRequestExceedsAvailableTicketCount() {
		Schedule schedule = scheduleWithTicketCounts(10, 3);

		boolean canPurchase = schedule.canPurchase(8);

		assertFalse(canPurchase);
	}

	@Test
	void canPurchaseReturnsFalseWhenRequestIsNotPositive() {
		Schedule schedule = scheduleWithTicketCounts(10, 3);

		assertFalse(schedule.canPurchase(0));
		assertFalse(schedule.canPurchase(-1));
	}

	private Schedule scheduleOn(LocalDate performanceDate) {
		return schedule(1L, performanceDate);
	}

	private Schedule schedule(Long id, LocalDate performanceDate) {
		return schedule(id, performanceDate, 1L);
	}

	private Schedule schedule(Long id, LocalDate performanceDate, Long performanceId) {
		LocalDateTime performanceDateTime = LocalDateTime.of(performanceDate, LocalTime.NOON);
		return Schedule.rehydrate(
			id,
			performanceDateTime,
			performanceDateTime.plusHours(1),
			10,
			0,
			ScheduleNumber.FIRST,
			performanceId
		);
	}

	private Schedule scheduleWithTicketCounts(int totalTicketCount, int allocatedTicketCount) {
		LocalDateTime performanceDate = LocalDateTime.of(2026, 5, 1, 12, 0);
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
