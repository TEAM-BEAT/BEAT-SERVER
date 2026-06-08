package com.beat.apis.external.notification.slack.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.beat.contracts.notification.BookingNotification;
import com.beat.contracts.notification.BookingNotificationPort;
import com.beat.apis.booking.application.dto.event.BookingCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCreatedEventListener {

	private final BookingNotificationPort bookingNotificationPort;

	@Async("beatAsyncExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendSlackNotification(BookingCreatedEvent event) {
		try {
			bookingNotificationPort.send(toNotification(event));
		} catch (Exception e) {
			log.error("Slack 전송 실패 - 공연: {}, 예매자: {}", event.performanceTitle(), event.bookerName(), e);
		}
	}

	private BookingNotification toNotification(BookingCreatedEvent event) {
		return new BookingNotification(
			event.bookingDateTime(),
			event.performanceTitle(),
			event.purchaseTicketCount(),
			event.bookerName(),
			event.scheduleDisplayName(),
			event.currentSoldTicketCount(),
			event.totalTicketCount()
		);
	}
}
