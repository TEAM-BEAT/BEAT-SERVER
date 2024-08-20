package com.beat.domain.performance.application.dto.update;

public record StaffAddRequest(
        String staffName,
        String staffRole,
        String staffPhoto
) {}