package com.beat.admin.user.api.response;

import com.beat.admin.user.application.result.AdminUserResults;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserFindAllResponse(
	@JsonProperty("users")
	List<UserFindResponse> userResponses
) {
	public static UserFindAllResponse from(List<UserFindResponse> userResponses) {
		return new UserFindAllResponse(userResponses);
	}

	public static UserFindAllResponse from(AdminUserResults results) {
		return new UserFindAllResponse(results.users().stream()
			.map(user -> UserFindResponse.of(user.id(), user.role()))
			.toList());
	}

	public record UserFindResponse(
		Long id,
		String role
	) {
		public static UserFindResponse of(Long id, String role) {
			return new UserFindResponse(id, role);
		}
	}
}
