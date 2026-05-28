package com.beat.apis.performance.application.dto.create;

import com.beat.global.support.jackson.CdnImageUrl;

public record CastResponse(
	Long castId,
	String castName,
	String castRole,
	@CdnImageUrl String castPhoto
) {
	public static CastResponse of(
		Long castId,
		String castName,
		String castRole,
		String castPhoto
	) {
		return new CastResponse(
			castId,
			castName,
			castRole,
			castPhoto
		);
	}
}
