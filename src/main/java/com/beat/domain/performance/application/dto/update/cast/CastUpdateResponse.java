package com.beat.domain.performance.application.dto.update.cast;

public record CastUpdateResponse(
        Long castId,
        String castName,
        String castRole,
        String castPhoto
) {
    public static CastUpdateResponse of(Long castId, String castName, String castRole, String castPhoto) {
        return new CastUpdateResponse(castId, castName, castRole, castPhoto);
    }
}
