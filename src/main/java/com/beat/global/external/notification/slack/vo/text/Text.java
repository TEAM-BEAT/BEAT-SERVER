package com.beat.global.external.notification.slack.vo.text;

public sealed interface Text permits PlainText, MarkdownText {
	String type();

	String text();
}
