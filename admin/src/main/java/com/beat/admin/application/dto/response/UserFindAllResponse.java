package com.beat.admin.application.dto.response;

import java.util.List;

import com.beat.admin.application.dto.result.AdminUserResult;

public record UserFindAllResponse(
	List<UserFindResponse> users
) {
	public static UserFindAllResponse from(List<AdminUserResult> users) {
		List<UserFindResponse> userFindResponses = users.stream()
			.map(UserFindResponse::from)
			.toList();
		return new UserFindAllResponse(userFindResponses);
	}

	public record UserFindResponse(
		Long id,
		String role
	) {
		public static UserFindResponse from(AdminUserResult user) {
			return new UserFindResponse(
				user.id(),
				user.role()
			);
		}
	}
}
