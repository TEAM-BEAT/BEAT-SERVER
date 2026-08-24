package com.beat.infrastructure.external.notification.slack.vo.text

import com.beat.infrastructure.external.notification.slack.vo.SlackConstant.TEXT_TYPE_PLAIN

internal data class PlainText(
    override val type: String,
    val text: String,
    val emoji: Boolean,
) : Text {
    companion object {
        fun newInstance(text: String): PlainText = PlainText(TEXT_TYPE_PLAIN, text, true)
    }
}
