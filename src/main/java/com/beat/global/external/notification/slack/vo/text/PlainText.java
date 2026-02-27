package com.beat.global.external.notification.slack.vo.text;

import static com.beat.global.external.notification.slack.vo.SlackConstant.TEXT_TYPE_PLAIN;

public record PlainText(
	String type,
	String text,
	boolean emoji
) implements Text {

	public static PlainText newInstance(String text) {
		return new PlainText(TEXT_TYPE_PLAIN, text, true);
	}
}
