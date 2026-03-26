package com.beat.domain.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Role {

	USER("ROLE_USER"),
	MEMBER("ROLE_MEMBER"),
	ADMIN("ROLE_ADMIN");

	private final String roleName;

	/**
	 * 역할 이름을 반환하는 메서드.
	 * 예: "ROLE_USER", "ROLE_MEMBER", "ROLE_ADMIN".
	 */
	public String getRoleName() {
		return this.roleName;
	}
}
