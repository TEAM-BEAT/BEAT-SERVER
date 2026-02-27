package com.beat.global.external.notification.slack.vo.block;

import static com.beat.global.external.notification.slack.vo.SlackConstant.BLOCK_TYPE_DIVIDER;

public record DividerBlock(
	String type
) implements Block {

	public static DividerBlock newInstance() {
		return new DividerBlock(BLOCK_TYPE_DIVIDER);
	}
}
