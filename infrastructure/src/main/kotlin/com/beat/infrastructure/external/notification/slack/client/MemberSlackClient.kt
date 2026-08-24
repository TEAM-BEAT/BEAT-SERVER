package com.beat.infrastructure.external.notification.slack.client

import com.beat.infrastructure.external.notification.slack.vo.message.SlackMessage
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "memberSlackClient", url = "\${slack.webhook.member-url}")
internal interface MemberSlackClient {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun sendMessage(@RequestBody payload: SlackMessage)
}
