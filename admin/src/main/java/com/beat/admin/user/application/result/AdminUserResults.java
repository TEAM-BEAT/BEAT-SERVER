package com.beat.admin.user.application.result;

import java.util.List;

public record AdminUserResults(List<AdminUserResult> users) {

	public AdminUserResults {
		users = List.copyOf(users);
	}

	public record AdminUserResult(Long id, String role) {
	}
}
