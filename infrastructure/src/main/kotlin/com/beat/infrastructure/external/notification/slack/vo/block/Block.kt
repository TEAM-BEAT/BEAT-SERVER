package com.beat.infrastructure.external.notification.slack.vo.block

internal sealed interface Block {
    val type: String
}
