package com.beat.domain.performance.application.dto;

public record MakerPerformanceDetail(
        Long performanceId,
        String genre,
        String performanceTitle,
        String posterImage,
        String performancePeriod,
        int minDueDate
) {
    public static MakerPerformanceDetail of(
            Long performanceId,
            String genre,
            String performanceTitle,
            String posterImage,
            String performancePeriod,
            int minDueDate) {  // minDueDate 매개변수 추가
        return new MakerPerformanceDetail(performanceId, genre, performanceTitle, posterImage, performancePeriod, minDueDate);
    }
}
