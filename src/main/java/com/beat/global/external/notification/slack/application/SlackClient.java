package com.beat.global.external.notification.slack.application;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "slackClient", url = "${slack.webhook.url}")
public interface SlackClient {

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	void sendMessage(@RequestBody Map<String, String> payload);
}
