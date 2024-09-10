package com.beat.domain.admin.application.dto;

public record UserFindResponse(
        Long id,
        String role
) {
    public static UserFindResponse of(Long id, String role) {
        return new UserFindResponse(id, role);
    }
}