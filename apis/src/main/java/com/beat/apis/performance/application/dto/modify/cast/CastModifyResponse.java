package com.beat.apis.performance.application.dto.modify.cast;

import com.beat.global.support.jackson.CdnImageUrl;

public record CastModifyResponse(Long castId,
								 String castName,
								 String castRole,
								 @CdnImageUrl String castPhoto) {

	public static CastModifyResponse of(Long castId, String castName, String castRole, String castPhoto) {
		return new CastModifyResponse(castId, castName, castRole, castPhoto);
	}
}
