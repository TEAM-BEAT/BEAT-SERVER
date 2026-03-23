package com.beat.infra.notification.slack.vo.message;

import com.beat.infra.notification.slack.vo.block.Block;
import java.util.List;

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
