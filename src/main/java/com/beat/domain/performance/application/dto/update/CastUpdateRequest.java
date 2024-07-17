package com.beat.domain.performance.application.dto.update;

public record CastUpdateRequest(
        Long castId,
        String castName,
        String castRole,
        String castPhoto
) {}
