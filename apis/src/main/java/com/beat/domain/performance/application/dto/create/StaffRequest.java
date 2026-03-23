package com.beat.domain.performance.application.dto.create;

public record StaffRequest(
	String staffName,
	String staffRole,
	String staffPhoto
) {
}