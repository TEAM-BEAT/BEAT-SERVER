package com.beat.domain.admin.application.dto;

import com.beat.domain.user.domain.Users;

import java.util.List;
import java.util.stream.Collectors;

public record UserFindAllResponse(
        List<UserResponse> users
) {
    public static UserFindAllResponse of(List<Users> users) {
        List<UserResponse> userResponses = users.stream()
                .map(user -> new UserResponse(user.getId(), user.getRole().getRoleName()))
                .collect(Collectors.toList());
        return new UserFindAllResponse(userResponses);
    }
}