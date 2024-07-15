package com.beat.domain.performance.application.dto;

public record MakerPerformanceDetail(
        Long performanceId,
        String genre,
        String performanceTitle,
        String posterImage,
        String performancePeriod
) {
    public static MakerPerformanceDetail of(
            Long performanceId,
            String genre,
            String performanceTitle,
            String posterImage,
            String performancePeriod) {
        return new MakerPerformanceDetail(performanceId, genre, performanceTitle, posterImage, performancePeriod);
    }
}
