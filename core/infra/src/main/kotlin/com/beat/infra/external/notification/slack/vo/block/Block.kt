package com.beat.infra.external.notification.slack.vo.block

internal sealed interface Block {
    val type: String
}
