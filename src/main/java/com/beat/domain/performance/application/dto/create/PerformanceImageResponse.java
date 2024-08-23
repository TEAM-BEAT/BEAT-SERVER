package com.beat.domain.performance.application.dto.create;

public record PerformanceImageResponse(
        Long imageId,
        String imageUrl
) {
    public static PerformanceImageResponse of(
            Long imageId,
            String imageUrl
    ) {
        return new PerformanceImageResponse(
                imageId,
                imageUrl
        );
    }
}
