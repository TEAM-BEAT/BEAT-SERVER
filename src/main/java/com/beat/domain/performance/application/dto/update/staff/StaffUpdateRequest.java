package com.beat.domain.performance.application.dto.update.staff;

public record StaffUpdateRequest(
        Long staffId,
        String staffName,
        String staffRole,
        String staffPhoto
) {}
