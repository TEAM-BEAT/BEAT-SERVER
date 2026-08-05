package com.beat.infra.external.notification.slack.vo.text

import com.beat.infra.external.notification.slack.vo.SlackConstant.TEXT_TYPE_MARKDOWN

data class MarkdownText(
    override val type: String,
    val text: String,
) : Text {
    companion object {
        fun newInstance(text: String): MarkdownText = MarkdownText(TEXT_TYPE_MARKDOWN, text)
    }
}
