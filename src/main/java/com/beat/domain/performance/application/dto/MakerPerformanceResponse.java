package com.beat.domain.performance.application.dto;

import java.util.List;

public record MakerPerformanceResponse(
        Long userId,
        List<MakerPerformanceDetail> performances
) {
    public static MakerPerformanceResponse of(
            Long userId,
            List<MakerPerformanceDetail> performances) {
        return new MakerPerformanceResponse(userId, performances);
    }
}
