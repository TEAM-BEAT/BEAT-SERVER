package com.beat.apis.common.application.converter;

import com.beat.apis.member.application.dto.request.SocialTypeRequest;
import com.beat.domain.member.domain.SocialType;

public final class SocialTypeEnumConverter {

	private SocialTypeEnumConverter() {
	}

	public static SocialType toDomain(final SocialTypeRequest socialTypeRequest) {
		if (socialTypeRequest == null) {
			return null;
		}

		return switch (socialTypeRequest) {
			case KAKAO -> SocialType.KAKAO;
		};
	}

}
