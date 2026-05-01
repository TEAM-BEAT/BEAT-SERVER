package com.beat.batch.booking.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.beat.batch.booking.facade.TicketCleanupFacade;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketCleanupJob {

	private final TicketCleanupFacade ticketCleanupFacade;

	@Value("${beat.scheduler.owner:false}")
	private boolean schedulerOwner;

	@Scheduled(cron = "0 0 4 * * ?")
	public void deleteOldCancelledBookings() {
		if (!schedulerOwner) {
			return;
		}

		ticketCleanupFacade.deleteOldCancelledBookings();
	}
}
