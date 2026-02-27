package com.beat.global.external.notification.slack.event;

import static com.beat.global.external.notification.slack.vo.SlackConstant.BRAND_COLOR;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.beat.domain.member.application.dto.event.MemberRegisteredEvent;
import com.beat.domain.member.port.in.MemberUseCase;
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
public class MemberRegisteredEventListener {

	private final MemberUseCase memberUseCase;
	private final SlackService slackService;

	@Async("taskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendSlackNotification(MemberRegisteredEvent event) {
		try {
			slackService.sendToMemberChannel(buildMessage(event));
		} catch (Exception e) {
			log.error("Slack 전송 실패 - 닉네임: {}", event.nickname(), e);
		}
	}

	private SlackMessage buildMessage(MemberRegisteredEvent event) {
		long memberCount = memberUseCase.countMembers();
		return SlackMessage.newInstance(
			List.of(
				HeaderBlock.newInstance("🎉 BEAT 신규 회원 가입 🎉"),
				SectionBlock.newInstanceWithFields(List.of(
					MarkdownText.newInstance("*🙋 닉네임*\n" + event.nickname()),
					MarkdownText.newInstance("*👥 누적 회원 수*\n" + memberCount + "명")
				)),
				DividerBlock.newInstance()
			),
			BRAND_COLOR
		);
	}
}
