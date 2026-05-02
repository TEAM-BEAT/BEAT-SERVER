package com.beat.admin.application.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserFindAllResponse(
	@JsonProperty("users")
	List<UserFindResponse> userResponses
) {
	public static UserFindAllResponse from(List<UserFindResponse> userResponses) {
		return new UserFindAllResponse(userResponses);
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
