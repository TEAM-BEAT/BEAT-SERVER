package com.beat.domain.performance.application.dto.update.cast;

public record CastAddResponse(
        Long castId,
        String castName,
        String castRole,
        String castPhoto
) {
    public static CastAddResponse of(Long castId, String castName, String castRole, String castPhoto) {
        return new CastAddResponse(castId, castName, castRole, castPhoto);
    }
}