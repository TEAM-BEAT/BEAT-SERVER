package com.beat.apis.performance.application.dto.modify.cast;

public record CastModifyResponse(Long castId,
								 String castName,
								 String castRole,
								 String castPhoto) {

	public static CastModifyResponse of(Long castId, String castName, String castRole, String castPhoto) {
		return new CastModifyResponse(castId, castName, castRole, castPhoto);
	}
}
