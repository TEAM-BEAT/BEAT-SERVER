package com.beat.infra.notification.slack.client;

import com.beat.infra.notification.slack.vo.message.SlackMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "memberSlackClient", url = "${slack.webhook.member-url}")
public interface MemberSlackClient {

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	void sendMessage(@RequestBody SlackMessage payload);
}
