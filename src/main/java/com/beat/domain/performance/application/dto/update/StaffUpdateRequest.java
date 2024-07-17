package com.beat.domain.performance.application.dto.update;

public record StaffUpdateRequest(
        Long staffId,
        String staffName,
        String staffRole,
        String staffPhoto
) {}
