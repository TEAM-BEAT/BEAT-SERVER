package com.beat.apis.performance.application.dto.performanceDetail;

import com.beat.global.support.jackson.CdnImageUrl;

public record PerformanceDetailStaffResponse(
	Long staffId,
	String staffName,
	String staffRole,
	@CdnImageUrl String staffPhoto
) {
	public static PerformanceDetailStaffResponse of(Long staffId, String staffName, String staffRole,
		String staffPhoto) {
		return new PerformanceDetailStaffResponse(staffId, staffName, staffRole, staffPhoto);
	}
}
