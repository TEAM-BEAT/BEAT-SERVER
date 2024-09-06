package com.beat.domain.admin.application.dto;

public record UserResponse(
        Long id,
        String role
) {
    public static UserResponse of(Long id, String role) {
        return new UserResponse(id, role);
    }
}