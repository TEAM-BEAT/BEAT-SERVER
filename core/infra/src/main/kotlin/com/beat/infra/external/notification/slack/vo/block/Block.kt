package com.beat.infra.external.notification.slack.vo.block

sealed interface Block {
    val type: String
}
