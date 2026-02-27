package com.beat.global.external.notification.slack.application;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SlackService {

	private final MemberSlackClient memberSlackClient;
	private final BookingSlackClient bookingSlackClient;

	public void sendToMemberChannel(Map<String, String> payload) {
		memberSlackClient.sendMessage(payload);
	}

	public void sendToBookingChannel(Map<String, String> payload) {
		bookingSlackClient.sendMessage(payload);
	}
}
