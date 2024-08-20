package com.beat.domain.performance.application.dto.update;

public record CastAddRequest(
        String castName,
        String castRole,
        String castPhoto
) {}