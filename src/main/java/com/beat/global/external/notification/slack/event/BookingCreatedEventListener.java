package com.beat.global.external.notification.slack.event;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.beat.domain.booking.application.dto.event.BookingCreatedEvent;
import com.beat.global.external.notification.slack.application.SlackService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCreatedEventListener {
	private static final String TEXT_KEY = "text";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final SlackService slackService;

	@Async("taskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendSlackNotification(BookingCreatedEvent event) {
		String message = formatMessage(event);
		Map<String, String> payload = Map.of(TEXT_KEY, message);

		try {
			slackService.sendToBookingChannel(payload);
		} catch (Exception e) {
			log.error("Slack 전송 실패 - 공연: {}, 예매자: {}", event.performanceTitle(), event.bookerName(), e);
		}
	}

	String formatMessage(BookingCreatedEvent event) {
		return """
			🎟️ BEAT 예매 발생 🎟️
			
			📅 예매일시: %s
			🎭 공연명: %s
			🔢 예매매수: %d매
			🙋 예매자: %s
			🔔 예매현황: %d/%d매""".formatted(
			event.bookingDateTime().format(DATE_FORMATTER),
			event.performanceTitle(),
			event.purchaseTicketCount(),
			event.bookerName(),
			event.currentSoldTicketCount(),
			event.totalTicketCount()
		);
	}
}
