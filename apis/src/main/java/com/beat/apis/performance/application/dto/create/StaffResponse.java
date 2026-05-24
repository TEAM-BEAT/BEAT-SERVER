package com.beat.apis.performance.application.dto.create;

import com.beat.global.support.jackson.CdnImageUrl;

public record StaffResponse(
	Long staffId,
	String staffName,
	String staffRole,
	@CdnImageUrl String staffPhoto
) {
	public static StaffResponse of(
		Long staffId,
		String staffName,
		String staffRole,
		String staffPhoto
	) {
		return new StaffResponse(
			staffId,
			staffName,
			staffRole,
			staffPhoto
		);
	}
}
