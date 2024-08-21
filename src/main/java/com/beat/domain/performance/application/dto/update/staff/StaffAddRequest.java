package com.beat.domain.performance.application.dto.update.staff;

public record StaffAddRequest(
        String staffName,
        String staffRole,
        String staffPhoto
) {}