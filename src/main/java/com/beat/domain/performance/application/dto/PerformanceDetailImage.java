package com.beat.domain.performance.application.dto;

public record PerformanceDetailImage(
        Long performanceImageId,
        String performanceImage
) {
    public static PerformanceDetailImage of(Long performanceImageId, String performanceImage) {
        return new PerformanceDetailImage(performanceImageId, performanceImage);
    }
}
