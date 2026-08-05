package com.beat.infra.external.notification.slack

import com.beat.contracts.notification.MemberNotification
import com.beat.contracts.notification.MemberNotificationPort
import com.beat.infra.external.notification.slack.client.MemberSlackClient
import com.beat.infra.external.notification.slack.vo.SlackConstant.BRAND_COLOR
import com.beat.infra.external.notification.slack.vo.block.DividerBlock
import com.beat.infra.external.notification.slack.vo.block.HeaderBlock
import com.beat.infra.external.notification.slack.vo.block.SectionBlock
import com.beat.infra.external.notification.slack.vo.message.SlackMessage
import com.beat.infra.external.notification.slack.vo.text.MarkdownText
import org.springframework.stereotype.Component

@Component
class SlackMemberNotificationAdapter(
    private val memberSlackClient: MemberSlackClient,
) : MemberNotificationPort {
    override fun send(notification: MemberNotification) {
        memberSlackClient.sendMessage(buildMessage(notification))
    }

    private fun buildMessage(notification: MemberNotification): SlackMessage =
        SlackMessage.newInstance(
            listOf(
                HeaderBlock.newInstance("🎉 BEAT 신규 회원 가입 🎉"),
                SectionBlock.newInstanceWithFields(
                    listOf(
                        MarkdownText.newInstance("*🙋 닉네임*\n${notification.nickname}"),
                        MarkdownText.newInstance("*👥 누적 회원 수*\n${notification.memberCount}명"),
                    ),
                ),
                DividerBlock.newInstance(),
            ),
            BRAND_COLOR,
        )
}
