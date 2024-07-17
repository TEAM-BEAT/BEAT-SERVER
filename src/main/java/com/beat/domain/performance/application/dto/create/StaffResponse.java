package com.beat.domain.performance.application.dto.create;

public record StaffResponse(
        Long staffId,
        String staffName,
        String staffRole,
        String staffPhoto
) {
    public static StaffResponse of(
            Long staffId,
            String staffName,
            String staffRole,
            String staffPhoto
    ) {
        return new StaffResponse(
                staffId,
                staffName,
                staffRole,
                staffPhoto
        );
    }
}