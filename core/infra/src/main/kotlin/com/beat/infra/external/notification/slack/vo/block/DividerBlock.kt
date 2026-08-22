package com.beat.infra.external.notification.slack.vo.block

import com.beat.infra.external.notification.slack.vo.SlackConstant.BLOCK_TYPE_DIVIDER

internal data class DividerBlock(
    override val type: String,
) : Block {
    companion object {
        fun newInstance(): DividerBlock = DividerBlock(BLOCK_TYPE_DIVIDER)
    }
}
