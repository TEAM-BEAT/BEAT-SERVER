package com.beat.global.external.notification.slack.application;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SlackService {

	private final SlackClient slackClient;

	public void sendMessage(Map<String, String> payload) {
		slackClient.sendMessage(payload);
	}
}
