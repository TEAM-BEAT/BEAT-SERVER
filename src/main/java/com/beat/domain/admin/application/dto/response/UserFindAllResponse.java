package com.beat.domain.admin.application.dto.response;

import com.beat.domain.user.domain.Users;

import java.util.List;

public record UserFindAllResponse(
	List<UserFindResponse> users
) {
	public static UserFindAllResponse from(List<Users> users) {
		List<UserFindResponse> userFindResponses = users.stream()
			.map(UserFindResponse::from)
			.toList();
		return new UserFindAllResponse(userFindResponses);
	}

	public record UserFindResponse(
		Long id,
		String role
	) {
		public static UserFindResponse from(Users user) {
			return new UserFindResponse(
				user.getId(),
				user.getRole().getRoleName()
			);
		}
	}
}