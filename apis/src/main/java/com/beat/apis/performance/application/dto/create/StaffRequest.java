package com.beat.apis.performance.application.dto.create;

public record StaffRequest(
	String staffName,
	String staffRole,
	String staffPhoto
) {
}
