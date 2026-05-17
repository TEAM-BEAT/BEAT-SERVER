package com.beat.batch.scheduler.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.domain.schedule.repository.ScheduleRepository;

@ExtendWith(MockitoExtension.class)
class JobSchedulerTransactionalServiceTest {

	private static final long SCHEDULE_ID = 1L;

	@Mock
	private ScheduleRepository scheduleRepository;

	private JobSchedulerTransactionalService jobSchedulerTransactionalService;

	@BeforeEach
	void setUp() {
		jobSchedulerTransactionalService = new JobSchedulerTransactionalService(scheduleRepository);
	}

	@Test
	void closeBookingThrowsWhenScheduleIsMissing() {
		when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> jobSchedulerTransactionalService.closeBooking(SCHEDULE_ID));

		verify(scheduleRepository).findById(SCHEDULE_ID);
		verifyNoMoreInteractions(scheduleRepository);
	}
}
