package com.beat.global.external.notification.slack.application;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "memberSlackClient", url = "${slack.webhook.member-url}")
public interface MemberSlackClient {

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	void sendMessage(@RequestBody Map<String, String> payload);
}
