package com.beat.infra.external.notification.slack.vo.block;

public sealed interface Block permits HeaderBlock, SectionBlock, DividerBlock {

	String type();
}
