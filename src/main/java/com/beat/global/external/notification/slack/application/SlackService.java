package com.beat.global.external.notification.slack.application;

import org.springframework.stereotype.Service;

import com.beat.global.external.notification.slack.vo.message.SlackMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SlackService {

	private final MemberSlackClient memberSlackClient;
	private final BookingSlackClient bookingSlackClient;

	public void sendToMemberChannel(SlackMessage payload) {
		memberSlackClient.sendMessage(payload);
	}

	public void sendToBookingChannel(SlackMessage payload) {
		bookingSlackClient.sendMessage(payload);
	}
}
