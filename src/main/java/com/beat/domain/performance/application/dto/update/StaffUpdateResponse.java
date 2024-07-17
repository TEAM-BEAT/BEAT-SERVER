package com.beat.domain.performance.application.dto.update;

public record StaffUpdateResponse(
        Long staffId,
        String staffName,
        String staffRole,
        String staffPhoto
) {
    public static StaffUpdateResponse of(Long staffId, String staffName, String staffRole, String staffPhoto) {
        return new StaffUpdateResponse(staffId, staffName, staffRole, staffPhoto);
    }
}