package com.beat.domain.performance.application.dto.performanceDetail;

public record PerformanceDetailStaffResponse(
	Long staffId,
	String staffName,
	String staffRole,
	String staffPhoto
) {
	public static PerformanceDetailStaffResponse of(Long staffId, String staffName, String staffRole,
		String staffPhoto) {
		return new PerformanceDetailStaffResponse(staffId, staffName, staffRole, staffPhoto);
	}
}
