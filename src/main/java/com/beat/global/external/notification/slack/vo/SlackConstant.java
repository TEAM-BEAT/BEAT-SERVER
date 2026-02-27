package com.beat.global.external.notification.slack.vo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlackConstant {
	public static final String BRAND_COLOR = "#FF006B";

	public static final String BLOCK_TYPE_HEADER = "header";
	public static final String BLOCK_TYPE_SECTION = "section";
	public static final String BLOCK_TYPE_DIVIDER = "divider";

	public static final String TEXT_TYPE_PLAIN = "plain_text";
	public static final String TEXT_TYPE_MARKDOWN = "mrkdwn";
}
