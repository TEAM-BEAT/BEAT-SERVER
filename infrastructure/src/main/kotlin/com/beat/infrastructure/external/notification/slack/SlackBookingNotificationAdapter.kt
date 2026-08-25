package com.beat.infrastructure.external.notification.slack

import com.beat.application.frontoffice.booking.booker.event.BookingCreatedEvent
import com.beat.application.frontoffice.booking.booker.event.BookingNotificationSender
import com.beat.infrastructure.external.notification.slack.client.BookingSlackClient
import com.beat.infrastructure.external.notification.slack.vo.SlackConstant.BRAND_COLOR
import com.beat.infrastructure.external.notification.slack.vo.block.DividerBlock
import com.beat.infrastructure.external.notification.slack.vo.block.HeaderBlock
import com.beat.infrastructure.external.notification.slack.vo.block.SectionBlock
import com.beat.infrastructure.external.notification.slack.vo.message.SlackMessage
import com.beat.infrastructure.external.notification.slack.vo.text.MarkdownText
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Component

@Component
internal class SlackBookingNotificationAdapter(private val bookingSlackClient: BookingSlackClient) :
    BookingNotificationSender {
    override fun send(event: BookingCreatedEvent) {
        bookingSlackClient.sendMessage(buildMessage(event))
    }

    private fun buildMessage(event: BookingCreatedEvent): SlackMessage =
        SlackMessage.newInstance(
            listOf(
                HeaderBlock.newInstance("🎟️ BEAT 예매 발생 🎟️"),
                SectionBlock.newInstanceWithFields(
                    listOf(
                        MarkdownText.newInstance(
                            "*📅 예매일시*\n${event.bookingDateTime.format(DATE_FORMATTER)}"
                        ),
                        MarkdownText.newInstance("*🎭 공연명*\n${event.performanceTitle}"),
                    )
                ),
                SectionBlock.newInstanceWithFields(
                    listOf(
                        MarkdownText.newInstance("*🔢 예매매수*\n${event.purchaseTicketCount}매"),
                        MarkdownText.newInstance("*🙋 예매자*\n${event.bookerName}"),
                    )
                ),
                SectionBlock.newInstanceWithFields(
                    listOf(
                        MarkdownText.newInstance("*🎬 회차*\n${event.scheduleDisplayName}"),
                        MarkdownText.newInstance(
                            "*🔔 예매현황*\n${event.currentSoldTicketCount}/${event.totalTicketCount}매"
                        ),
                    )
                ),
                DividerBlock.newInstance(),
            ),
            BRAND_COLOR,
        )

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
