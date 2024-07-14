package com.beat.domain.performance.application.dto;

public record PerformanceDetailCast(
        Long castId,
        String castName,
        String castRole,
        String castPhoto
) {
    public static PerformanceDetailCast of(Long castId, String castName, String castRole, String castPhoto) {
        return new PerformanceDetailCast(castId, castName, castRole, castPhoto);
    }
}
