package com.beat.batch.booking.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

import com.beat.batch.booking.facade.TicketCleanupFacade;

class TicketCleanupJobTest {

	@Test
	void scheduledCleanupKeepsCronContract() throws NoSuchMethodException {
		Scheduled scheduled = TicketCleanupJob.class
			.getDeclaredMethod("deleteOldCancelledBookings")
			.getAnnotation(Scheduled.class);

		assertEquals("0 0 4 * * ?", scheduled.cron());
	}

	@Test
	void scheduledCleanupDelegatesWhenRuntimeOwnsScheduler() {
		TicketCleanupFacade ticketCleanupFacade = mock(TicketCleanupFacade.class);
		TicketCleanupJob ticketCleanupJob = new TicketCleanupJob(ticketCleanupFacade);
		ReflectionTestUtils.setField(ticketCleanupJob, "schedulerOwner", true);

		ticketCleanupJob.deleteOldCancelledBookings();

		verify(ticketCleanupFacade).deleteOldCancelledBookings();
	}

	@Test
	void scheduledCleanupSkipsWhenRuntimeIsNotSchedulerOwner() {
		TicketCleanupFacade ticketCleanupFacade = mock(TicketCleanupFacade.class);
		TicketCleanupJob ticketCleanupJob = new TicketCleanupJob(ticketCleanupFacade);
		ReflectionTestUtils.setField(ticketCleanupJob, "schedulerOwner", false);

		ticketCleanupJob.deleteOldCancelledBookings();

		verifyNoInteractions(ticketCleanupFacade);
	}
}
