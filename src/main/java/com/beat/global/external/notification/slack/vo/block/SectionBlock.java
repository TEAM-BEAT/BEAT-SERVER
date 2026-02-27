package com.beat.global.external.notification.slack.vo.block;

import static com.beat.global.external.notification.slack.vo.SlackConstant.BLOCK_TYPE_SECTION;

import java.util.List;

import com.beat.global.external.notification.slack.vo.text.Text;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SectionBlock(
	String type,
	List<Text> fields,
	Text text
) implements Block {

	public static SectionBlock newInstanceWithFields(List<Text> fields) {
		return new SectionBlock(BLOCK_TYPE_SECTION, fields, null);
	}

	public static SectionBlock newInstanceWithText(Text text) {
		return new SectionBlock(BLOCK_TYPE_SECTION, null, text);
	}
}
