package com.beat.global.external.notification.slack.application;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SlackService {

	private final SlackClient slackClient;

	/**
	 * Sends a Slack message using the provided payload.
	 *
	 * This method forwards the message payload to the Slack client.
	 *
	 * @param payload a map containing key-value pairs representing the details of the Slack message
	 */
	public void sendMessage(Map<String, String> payload) {
		slackClient.sendMessage(payload);
	}
}
