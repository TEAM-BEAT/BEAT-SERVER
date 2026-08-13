package com.beat.infra.external.notification.slack.vo.block

import com.beat.infra.external.notification.slack.vo.SlackConstant.BLOCK_TYPE_HEADER
import com.beat.infra.external.notification.slack.vo.text.PlainText
import com.beat.infra.external.notification.slack.vo.text.Text

data class HeaderBlock(
    override val type: String,
    val text: Text,
) : Block {
    companion object {
        fun newInstance(text: String): HeaderBlock = HeaderBlock(BLOCK_TYPE_HEADER, PlainText.newInstance(text))
    }
}
