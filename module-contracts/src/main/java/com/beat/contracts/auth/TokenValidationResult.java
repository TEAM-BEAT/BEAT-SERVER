package com.beat.contracts.auth;

public enum TokenValidationResult {
	VALID,
	INVALID_SIGNATURE,
	INVALID_TOKEN,
	EXPIRED,
	UNSUPPORTED,
	EMPTY
}
