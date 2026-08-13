package com.beat.infra.external.notification.slack.vo.block

import com.beat.infra.external.notification.slack.vo.SlackConstant.BLOCK_TYPE_SECTION
import com.beat.infra.external.notification.slack.vo.text.Text
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SectionBlock(
    override val type: String,
    val fields: List<Text>?,
    val text: Text?,
) : Block {
    companion object {
        fun newInstanceWithFields(fields: List<Text>): SectionBlock = SectionBlock(BLOCK_TYPE_SECTION, fields, null)

        fun newInstanceWithText(text: Text): SectionBlock = SectionBlock(BLOCK_TYPE_SECTION, null, text)
    }
}
