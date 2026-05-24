package com.beat.apis.performance.application.dto.modify.staff;

import com.beat.global.support.jackson.CdnImageUrl;

public record StaffModifyResponse(
	Long staffId,
	String staffName,
	String staffRole,
	@CdnImageUrl String staffPhoto) {
	public static StaffModifyResponse of(Long staffId, String staffName, String staffRole, String staffPhoto) {
		return new StaffModifyResponse(staffId, staffName, staffRole, staffPhoto);
	}
}
