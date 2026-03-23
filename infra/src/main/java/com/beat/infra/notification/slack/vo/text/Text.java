package com.beat.infra.notification.slack.vo.text;

public sealed interface Text permits PlainText, MarkdownText {

	String type();

	String text();
}
