package com.beat.global.external.notification.slack.vo.text;

import static com.beat.global.external.notification.slack.vo.SlackConstant.TEXT_TYPE_MARKDOWN;

public record MarkdownText(
	String type,
	String text
) implements Text {

	public static MarkdownText newInstance(String text) {
		return new MarkdownText(TEXT_TYPE_MARKDOWN, text);
	}
}
