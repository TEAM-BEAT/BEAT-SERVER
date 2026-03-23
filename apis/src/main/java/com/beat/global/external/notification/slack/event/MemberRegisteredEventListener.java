package com.beat.global.external.notification.slack.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.beat.contracts.notification.MemberNotification;
import com.beat.contracts.notification.MemberNotificationPort;
import com.beat.domain.member.application.dto.event.MemberRegisteredEvent;
import com.beat.domain.member.port.in.MemberUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberRegisteredEventListener {

	private final MemberUseCase memberUseCase;
	private final MemberNotificationPort memberNotificationPort;

	@Async("taskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendSlackNotification(MemberRegisteredEvent event) {
		try {
			memberNotificationPort.send(toNotification(event));
		} catch (Exception e) {
			log.error("Slack 전송 실패 - 닉네임: {}", event.nickname(), e);
		}
	}

	private MemberNotification toNotification(MemberRegisteredEvent event) {
		return new MemberNotification(event.nickname(), memberUseCase.countMembers());
	}
}
