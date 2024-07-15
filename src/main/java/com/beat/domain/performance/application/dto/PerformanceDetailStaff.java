package com.beat.domain.performance.application.dto;

public record PerformanceDetailStaff(
        Long staffId,
        String staffName,
        String staffRole,
        String staffPhoto
) {
    public static PerformanceDetailStaff of(Long staffId, String staffName, String staffRole, String staffPhoto) {
        return new PerformanceDetailStaff(staffId, staffName, staffRole, staffPhoto);
    }
}
