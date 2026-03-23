package com.beat.infra.notification.slack.vo.block;

import static com.beat.infra.notification.slack.vo.SlackConstant.BLOCK_TYPE_SECTION;

import com.beat.infra.notification.slack.vo.text.Text;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

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
