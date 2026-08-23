package com.beat.infra.external.notification.slack.vo.message

import com.beat.infra.external.notification.slack.vo.block.Block

internal data class SlackMessage(
    val attachments: List<Attachment>,
) {
    companion object {
        fun newInstance(blocks: List<Block>, color: String): SlackMessage =
            SlackMessage(listOf(Attachment.of(color, blocks)))
    }

    data class Attachment(
        val color: String,
        val blocks: List<Block>,
    ) {
        companion object {
            fun of(color: String, blocks: List<Block>): Attachment = Attachment(color, blocks)
        }
    }
}
