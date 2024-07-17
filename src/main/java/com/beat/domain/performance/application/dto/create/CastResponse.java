package com.beat.domain.performance.application.dto.create;

public record CastResponse(
        Long castId,
        String castName,
        String castRole,
        String castPhoto
) {
    public static CastResponse of(
            Long castId,
            String castName,
            String castRole,
            String castPhoto
    ) {
        return new CastResponse(
                castId,
                castName,
                castRole,
                castPhoto
        );
    }
}