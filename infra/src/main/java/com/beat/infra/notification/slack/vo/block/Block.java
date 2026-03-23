package com.beat.infra.notification.slack.vo.block;

public sealed interface Block permits HeaderBlock, SectionBlock, DividerBlock {

	String type();
}
