package com.beat.batch.booking.job;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.booking.application.TicketCleanupScheduler;

class TicketCleanupJobTest {

	@Test
	void scheduledCleanupDelegatesWhenRuntimeOwnsScheduler() {
		TicketCleanupScheduler ticketCleanupScheduler = mock(TicketCleanupScheduler.class);
		TicketCleanupJob ticketCleanupJob = new TicketCleanupJob(ticketCleanupScheduler);
		ReflectionTestUtils.setField(ticketCleanupJob, "schedulerOwner", true);

		ticketCleanupJob.deleteOldCancelledBookings();

		verify(ticketCleanupScheduler).deleteOldCancelledBookings();
	}

	@Test
	void scheduledCleanupSkipsWhenRuntimeIsNotSchedulerOwner() {
		TicketCleanupScheduler ticketCleanupScheduler = mock(TicketCleanupScheduler.class);
		TicketCleanupJob ticketCleanupJob = new TicketCleanupJob(ticketCleanupScheduler);
		ReflectionTestUtils.setField(ticketCleanupJob, "schedulerOwner", false);

		ticketCleanupJob.deleteOldCancelledBookings();

		verifyNoInteractions(ticketCleanupScheduler);
	}
}
