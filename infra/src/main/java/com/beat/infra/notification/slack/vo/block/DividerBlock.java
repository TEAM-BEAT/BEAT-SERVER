package com.beat.infra.notification.slack.vo.block;

import static com.beat.infra.notification.slack.vo.SlackConstant.BLOCK_TYPE_DIVIDER;

public record DividerBlock(
	String type
) implements Block {

	public static DividerBlock newInstance() {
		return new DividerBlock(BLOCK_TYPE_DIVIDER);
	}
}
