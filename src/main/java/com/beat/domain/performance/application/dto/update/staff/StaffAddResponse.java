package com.beat.domain.performance.application.dto.update.staff;

public record StaffAddResponse(
        Long staffId,
        String staffName,
        String staffRole,
        String staffPhoto
) {
    public static StaffAddResponse of(Long staffId, String staffName, String staffRole, String staffPhoto) {
        return new StaffAddResponse(staffId, staffName, staffRole, staffPhoto);
    }
}