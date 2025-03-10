package com.beat.global.external.notification.slack.application;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "slackClient", url = "${slack.webhook.url}")
public interface SlackClient {

	/**
	 * Sends a JSON payload as a Slack message to the configured webhook.
	 *
	 * <p>This method posts the provided payload to the Slack webhook specified in the application properties.
	 * The payload should contain key-value pairs representing the Slack message details.
	 *
	 * @param payload JSON payload containing details of the Slack message
	 */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	void sendMessage(@RequestBody Map<String, String> payload);
}
