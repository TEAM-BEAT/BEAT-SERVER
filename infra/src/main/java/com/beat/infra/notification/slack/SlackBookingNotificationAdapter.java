package com.beat.infra.notification.slack;

import static com.beat.infra.notification.slack.vo.SlackConstant.BRAND_COLOR;

import com.beat.contracts.notification.BookingNotification;
import com.beat.contracts.notification.BookingNotificationPort;
import com.beat.infra.notification.slack.client.BookingSlackClient;
import com.beat.infra.notification.slack.vo.block.DividerBlock;
import com.beat.infra.notification.slack.vo.block.HeaderBlock;
import com.beat.infra.notification.slack.vo.block.SectionBlock;
import com.beat.infra.notification.slack.vo.message.SlackMessage;
import com.beat.infra.notification.slack.vo.text.MarkdownText;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlackBookingNotificationAdapter implements BookingNotificationPort {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final BookingSlackClient bookingSlackClient;

	@Override
	public void send(BookingNotification notification) {
		bookingSlackClient.sendMessage(buildMessage(notification));
	}

	private SlackMessage buildMessage(BookingNotification notification) {
		return SlackMessage.newInstance(
			List.of(
				HeaderBlock.newInstance("🎟️ BEAT 예매 발생 🎟️"),
				SectionBlock.newInstanceWithFields(List.of(
					MarkdownText.newInstance("*📅 예매일시*\n" + notification.bookingDateTime().format(DATE_FORMATTER)),
					MarkdownText.newInstance("*🎭 공연명*\n" + notification.performanceTitle())
				)),
				SectionBlock.newInstanceWithFields(List.of(
					MarkdownText.newInstance("*🔢 예매매수*\n" + notification.purchaseTicketCount() + "매"),
					MarkdownText.newInstance("*🙋 예매자*\n" + notification.bookerName())
				)),
				SectionBlock.newInstanceWithFields(List.of(
					MarkdownText.newInstance("*🎬 회차*\n" + notification.scheduleDisplayName()),
					MarkdownText.newInstance(
						"*🔔 예매현황*\n" + notification.currentSoldTicketCount() + "/" + notification.totalTicketCount() + "매")
				)),
				DividerBlock.newInstance()
			),
			BRAND_COLOR
		);
	}
}
