package com.beat.apis.performance.application.dto.create;

public record CastRequest(
	String castName,
	String castRole,
	String castPhoto
) {
}
