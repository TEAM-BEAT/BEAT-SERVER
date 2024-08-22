package com.beat.domain.performance.application.dto.update.cast;

public record CastDeleteResponse(
        Long castId
) {
    public static CastDeleteResponse from(Long castId) {
        return new CastDeleteResponse(castId);
    }
}