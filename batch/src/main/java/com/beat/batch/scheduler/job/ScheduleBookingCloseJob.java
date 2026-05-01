package com.beat.batch.scheduler.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.beat.batch.scheduler.facade.ScheduleBookingCloseFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleBookingCloseJob {

	private final ScheduleBookingCloseFacade scheduleBookingCloseFacade;

	@Value("${beat.scheduler.owner:false}")
	private boolean schedulerOwner;

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		if (!schedulerOwner) {
			log.info("Skipping schedule rehydration because this runtime is not the scheduler owner.");
			return;
		}

		log.info("onApplicationReady() method triggered.");
		scheduleBookingCloseFacade.reconcilePendingSchedules();
	}

	@Scheduled(fixedDelayString = "${beat.scheduler.reconcile-interval-ms:60000}")
	public void reconcilePendingSchedules() {
		if (!schedulerOwner) {
			return;
		}

		scheduleBookingCloseFacade.reconcilePendingSchedules();
	}
}
