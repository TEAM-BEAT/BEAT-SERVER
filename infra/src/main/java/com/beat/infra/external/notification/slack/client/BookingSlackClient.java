package com.beat.infra.external.notification.slack.client;

import com.beat.infra.external.notification.slack.vo.message.SlackMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "bookingSlackClient", url = "${slack.webhook.booking-url}")
public interface BookingSlackClient {

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	void sendMessage(@RequestBody SlackMessage payload);
}
