package com.beat.infra.external.notification.slack;

import static com.beat.infra.external.notification.slack.vo.SlackConstant.BRAND_COLOR;

import com.beat.contracts.notification.MemberNotification;
import com.beat.contracts.notification.MemberNotificationPort;
import com.beat.infra.external.notification.slack.client.MemberSlackClient;
import com.beat.infra.external.notification.slack.vo.block.DividerBlock;
import com.beat.infra.external.notification.slack.vo.block.HeaderBlock;
import com.beat.infra.external.notification.slack.vo.block.SectionBlock;
import com.beat.infra.external.notification.slack.vo.message.SlackMessage;
import com.beat.infra.external.notification.slack.vo.text.MarkdownText;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlackMemberNotificationAdapter implements MemberNotificationPort {

	private final MemberSlackClient memberSlackClient;

	@Override
	public void send(MemberNotification notification) {
		memberSlackClient.sendMessage(buildMessage(notification));
	}

	private SlackMessage buildMessage(MemberNotification notification) {
		return SlackMessage.newInstance(
			List.of(
				HeaderBlock.newInstance("🎉 BEAT 신규 회원 가입 🎉"),
				SectionBlock.newInstanceWithFields(List.of(
					MarkdownText.newInstance("*🙋 닉네임*\n" + notification.getNickname()),
					MarkdownText.newInstance("*👥 누적 회원 수*\n" + notification.getMemberCount() + "명")
				)),
				DividerBlock.newInstance()
			),
			BRAND_COLOR
		);
	}
}
