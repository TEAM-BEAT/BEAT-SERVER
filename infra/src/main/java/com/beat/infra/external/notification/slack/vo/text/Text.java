package com.beat.infra.external.notification.slack.vo.text;

public sealed interface Text permits PlainText, MarkdownText {

	String type();

	String text();
}
