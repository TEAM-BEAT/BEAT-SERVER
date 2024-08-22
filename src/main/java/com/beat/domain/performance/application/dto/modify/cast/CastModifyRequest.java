package com.beat.domain.performance.application.dto.modify.cast;

import org.jetbrains.annotations.Nullable;

public record CastModifyRequest(
        @Nullable
        Long castId,
        String castName,
        String castRole,
        String castPhoto
) {
}