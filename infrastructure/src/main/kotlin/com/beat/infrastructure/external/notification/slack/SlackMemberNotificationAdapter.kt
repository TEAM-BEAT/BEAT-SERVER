package com.beat.infrastructure.external.notification.slack

import com.beat.application.frontoffice.member.command.MemberRegistrationNotification
import com.beat.application.frontoffice.member.command.MemberRegistrationNotifier
import com.beat.infrastructure.external.notification.slack.client.MemberSlackClient
import com.beat.infrastructure.external.notification.slack.vo.SlackConstant.BRAND_COLOR
import com.beat.infrastructure.external.notification.slack.vo.block.DividerBlock
import com.beat.infrastructure.external.notification.slack.vo.block.HeaderBlock
import com.beat.infrastructure.external.notification.slack.vo.block.SectionBlock
import com.beat.infrastructure.external.notification.slack.vo.message.SlackMessage
import com.beat.infrastructure.external.notification.slack.vo.text.MarkdownText
import org.springframework.stereotype.Component

@Component
internal class SlackMemberNotificationAdapter(private val memberSlackClient: MemberSlackClient) :
    MemberRegistrationNotifier {
    override fun send(notification: MemberRegistrationNotification) {
        memberSlackClient.sendMessage(buildMessage(notification))
    }

    private fun buildMessage(notification: MemberRegistrationNotification): SlackMessage =
        SlackMessage.newInstance(
            listOf(
                HeaderBlock.newInstance("🎉 BEAT 신규 회원 가입 🎉"),
                SectionBlock.newInstanceWithFields(
                    listOf(
                        MarkdownText.newInstance("*🙋 닉네임*\n${notification.nickname}"),
                        MarkdownText.newInstance("*👥 누적 회원 수*\n${notification.memberCount}명"),
                    )
                ),
                DividerBlock.newInstance(),
            ),
            BRAND_COLOR,
        )
}
