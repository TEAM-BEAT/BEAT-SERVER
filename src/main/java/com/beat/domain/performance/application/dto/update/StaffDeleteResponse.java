package com.beat.domain.performance.application.dto.update;

public record StaffDeleteResponse(
        Long staffId
) {
    public static StaffDeleteResponse from(Long staffId) {
        return new StaffDeleteResponse(staffId);
    }
}