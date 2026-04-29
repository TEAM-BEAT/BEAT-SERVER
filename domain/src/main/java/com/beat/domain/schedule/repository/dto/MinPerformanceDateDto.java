package com.beat.domain.schedule.repository.dto;

import java.time.LocalDateTime;

public class MinPerformanceDateDto {
    private final Long performanceId;
    private final LocalDateTime performanceDate;

    public MinPerformanceDateDto(Long performanceId, LocalDateTime performanceDate) {
        this.performanceId = performanceId;
        this.performanceDate = performanceDate;
    }

    public Long getPerformanceId() {
        return performanceId;
    }

    public LocalDateTime getPerformanceDate() {
        return performanceDate;
    }
}
