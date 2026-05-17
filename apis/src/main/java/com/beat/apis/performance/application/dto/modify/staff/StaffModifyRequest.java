package com.beat.apis.performance.application.dto.modify.staff;

import org.jetbrains.annotations.Nullable;

public record StaffModifyRequest(
	@Nullable
	Long staffId,
	String staffName,
	String staffRole,
	String staffPhoto
) {
}
