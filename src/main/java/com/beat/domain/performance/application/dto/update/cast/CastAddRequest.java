package com.beat.domain.performance.application.dto.update.cast;

public record CastAddRequest(
        String castName,
        String castRole,
        String castPhoto
) {}