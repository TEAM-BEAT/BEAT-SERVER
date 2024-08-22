package com.beat.domain.performance.application.dto.update.staff;

public record StaffDeleteResponse(
        Long staffId
) {
    public static StaffDeleteResponse from(Long staffId) {
        return new StaffDeleteResponse(staffId);
    }
}