package com.beat.domain.admin.application.dto;

import com.beat.domain.user.domain.Users;

import java.util.List;

public record UserFindAllResponse(
        List<Users> user
) {
    public static UserFindAllResponse of(List<Users> user) {
        return new UserFindAllResponse(user);
    }
}