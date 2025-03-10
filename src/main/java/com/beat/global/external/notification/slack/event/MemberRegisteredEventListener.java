package com.beat.global.external.notification.slack.event;

import java.util.HashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.beat.domain.member.application.dto.event.MemberRegisteredEvent;
import com.beat.domain.member.port.in.MemberUseCase;
import com.beat.global.external.notification.slack.application.SlackService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberRegisteredEventListener {
	private static final String TEXT_KEY = "text";
	private static final String WELCOME_MESSAGE = "번째 유저가 회원가입했띠예 🎉🎉 - ";
	private static final String SLACK_TRANSFER_ERROR = "Slack 전송 실패";

	private final MemberUseCase memberUseCase;
	private final SlackService slackService;

	/**
	 * Asynchronously sends a Slack notification when a new member registers.
	 *
	 * <p>This method is triggered after a transaction commits and builds a payload with a welcome
	 * message that includes the current number of members and the new member's nickname. It then tries
	 * to send this payload to Slack. If the message fails to send, a RuntimeException is thrown.
	 *
	 * @param event the member registration event containing details about the new member
	 */
	@Async("taskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendSlackNotification(MemberRegisteredEvent event) {
		Map<String, String> payload = new HashMap<>();
		payload.put(TEXT_KEY, memberUseCase.countMembers() + WELCOME_MESSAGE + event.nickname());

		try {
			slackService.sendMessage(payload);
		} catch (Exception e) {
			throw new RuntimeException(SLACK_TRANSFER_ERROR);
		}
	}
}
