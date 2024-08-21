package com.beat.domain.performance.application.dto.update.cast;

public record CastUpdateRequest(
        Long castId,
        String castName,
        String castRole,
        String castPhoto
) {}
