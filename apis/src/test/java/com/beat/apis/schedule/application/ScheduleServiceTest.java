package com.beat.apis.schedule.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.schedule.application.dto.response.MinPerformanceDateResponse;
import com.beat.contracts.schedule.ScheduleReadPort;
import com.beat.contracts.schedule.readmodel.MinPerformanceDateReadModel;
import com.beat.domain.schedule.repository.ScheduleRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private ScheduleReadPort scheduleReadPort;

	private ScheduleService scheduleService;

	@BeforeEach
	void setUp() {
		scheduleService = new ScheduleService(scheduleRepository, scheduleReadPort);
	}

	@Test
	void retrieveMinPerformanceDateUsesScheduleReadPortAndPreservesResponseMap() {
		List<Long> performanceIds = List.of(1L, 2L);
		LocalDateTime firstPerformanceDate = LocalDateTime.of(2026, 5, 1, 19, 30);
		LocalDateTime secondPerformanceDate = LocalDateTime.of(2026, 5, 2, 20, 0);
		when(scheduleReadPort.findMinPerformanceDateByPerformanceIds(performanceIds))
			.thenReturn(List.of(
				new MinPerformanceDateReadModel(1L, firstPerformanceDate),
				new MinPerformanceDateReadModel(2L, secondPerformanceDate)
			));

		MinPerformanceDateResponse response = scheduleService.retrieveMinPerformanceDateByPerformanceIds(performanceIds);

		assertEquals(
			Map.of(1L, firstPerformanceDate, 2L, secondPerformanceDate),
			response.performanceDateMap()
		);
		verify(scheduleReadPort).findMinPerformanceDateByPerformanceIds(performanceIds);
	}

	@Test
	void retrieveMinPerformanceDateKeepsFirstValueWhenReadPortReturnsDuplicatePerformanceId() {
		List<Long> performanceIds = List.of(1L);
		LocalDateTime firstPerformanceDate = LocalDateTime.of(2026, 5, 1, 19, 30);
		LocalDateTime duplicatePerformanceDate = LocalDateTime.of(2026, 5, 2, 20, 0);
		when(scheduleReadPort.findMinPerformanceDateByPerformanceIds(performanceIds))
			.thenReturn(List.of(
				new MinPerformanceDateReadModel(1L, firstPerformanceDate),
				new MinPerformanceDateReadModel(1L, duplicatePerformanceDate)
			));

		MinPerformanceDateResponse response = scheduleService.retrieveMinPerformanceDateByPerformanceIds(performanceIds);

		assertEquals(Map.of(1L, firstPerformanceDate), response.performanceDateMap());
	}
}
