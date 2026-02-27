package com.beat.global.external.notification.slack.event;

import static com.beat.global.external.notification.slack.vo.SlackConstant.BRAND_COLOR;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.beat.domain.booking.application.dto.event.BookingCreatedEvent;
import com.beat.global.external.notification.slack.application.SlackService;
import com.beat.global.external.notification.slack.vo.block.DividerBlock;
import com.beat.global.external.notification.slack.vo.block.HeaderBlock;
import com.beat.global.external.notification.slack.vo.block.SectionBlock;
import com.beat.global.external.notification.slack.vo.message.SlackMessage;
import com.beat.global.external.notification.slack.vo.text.MarkdownText;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCreatedEventListener {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final SlackService slackService;

	@Async("taskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendSlackNotification(BookingCreatedEvent event) {
		try {
			slackService.sendToBookingChannel(buildMessage(event));
		} catch (Exception e) {
			log.error("Slack 전송 실패 - 공연: {}, 예매자: {}", event.performanceTitle(), event.bookerName(), e);
		}
	}

	private SlackMessage buildMessage(BookingCreatedEvent event) {
		return SlackMessage.newInstance(
			List.of(
				HeaderBlock.newInstance("🎟️ BEAT 예매 발생 🎟️"),
				SectionBlock.newInstanceWithFields(List.of(
					MarkdownText.newInstance("*📅 예매일시*\n" + event.bookingDateTime().format(DATE_FORMATTER)),
					MarkdownText.newInstance("*🎭 공연명*\n" + event.performanceTitle())
				)),
				SectionBlock.newInstanceWithFields(List.of(
					MarkdownText.newInstance("*🔢 예매매수*\n" + event.purchaseTicketCount() + "매"),
					MarkdownText.newInstance("*🙋 예매자*\n" + event.bookerName())
				)),
				SectionBlock.newInstanceWithFields(List.of(
					MarkdownText.newInstance(
						"*🔔 예매현황*\n" + event.currentSoldTicketCount() + "/" + event.totalTicketCount() + "매")
				)),
				DividerBlock.newInstance()
			),
			BRAND_COLOR
		);
	}
}
