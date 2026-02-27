package com.beat.global.external.notification.slack.vo.message;

import java.util.List;

import com.beat.global.external.notification.slack.vo.block.Block;

public record SlackMessage(
	List<Attachment> attachments
) {

	public static SlackMessage newInstance(List<Block> blocks, String color) {
		return new SlackMessage(List.of(Attachment.of(color, blocks)));
	}

	public record Attachment(String color, List<Block> blocks) {

		public static Attachment of(String color, List<Block> blocks) {
			return new Attachment(color, blocks);
		}
	}
}
