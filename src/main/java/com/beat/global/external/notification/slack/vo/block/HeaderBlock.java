package com.beat.global.external.notification.slack.vo.block;

import static com.beat.global.external.notification.slack.vo.SlackConstant.BLOCK_TYPE_HEADER;

import com.beat.global.external.notification.slack.vo.text.PlainText;
import com.beat.global.external.notification.slack.vo.text.Text;

public record HeaderBlock(
	String type,
	Text text
) implements Block {

	public static HeaderBlock newInstance(String text) {
		return new HeaderBlock(BLOCK_TYPE_HEADER, PlainText.newInstance(text));
	}
}
