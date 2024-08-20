package com.beat.domain.performance.application.dto.update;

public record CastDeleteResponse(
        Long castId
) {
    public static CastDeleteResponse from(Long castId) {
        return new CastDeleteResponse(castId);
    }
}