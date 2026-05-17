package com.beat.contracts.auth;

public record JwtSubject(
	Long memberId,
	String roleName
) {
}
