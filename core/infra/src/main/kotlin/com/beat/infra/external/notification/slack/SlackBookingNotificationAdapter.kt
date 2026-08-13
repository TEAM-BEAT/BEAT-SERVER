package com.beat.infra.external.notification.slack

import com.beat.contracts.notification.BookingNotification
import com.beat.contracts.notification.BookingNotificationPort
import com.beat.infra.external.notification.slack.client.BookingSlackClient
import com.beat.infra.external.notification.slack.vo.SlackConstant.BRAND_COLOR
import com.beat.infra.external.notification.slack.vo.block.DividerBlock
import com.beat.infra.external.notification.slack.vo.block.HeaderBlock
import com.beat.infra.external.notification.slack.vo.block.SectionBlock
import com.beat.infra.external.notification.slack.vo.message.SlackMessage
import com.beat.infra.external.notification.slack.vo.text.MarkdownText
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class SlackBookingNotificationAdapter(
    private val bookingSlackClient: BookingSlackClient,
) : BookingNotificationPort {
    override fun send(notification: BookingNotification) {
        bookingSlackClient.sendMessage(buildMessage(notification))
    }

    private fun buildMessage(notification: BookingNotification): SlackMessage =
        SlackMessage.newInstance(
            listOf(
                HeaderBlock.newInstance("🎟️ BEAT 예매 발생 🎟️"),
                SectionBlock.newInstanceWithFields(
                    listOf(
                        MarkdownText.newInstance("*📅 예매일시*\n${notification.bookingDateTime.format(DATE_FORMATTER)}"),
                        MarkdownText.newInstance("*🎭 공연명*\n${notification.performanceTitle}"),
                    ),
                ),
                SectionBlock.newInstanceWithFields(
                    listOf(
                        MarkdownText.newInstance("*🔢 예매매수*\n${notification.purchaseTicketCount}매"),
                        MarkdownText.newInstance("*🙋 예매자*\n${notification.bookerName}"),
                    ),
                ),
                SectionBlock.newInstanceWithFields(
                    listOf(
                        MarkdownText.newInstance("*🎬 회차*\n${notification.scheduleDisplayName}"),
                        MarkdownText.newInstance(
                            "*🔔 예매현황*\n${notification.currentSoldTicketCount}/${notification.totalTicketCount}매",
                        ),
                    ),
                ),
                DividerBlock.newInstance(),
            ),
            BRAND_COLOR,
        )

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
