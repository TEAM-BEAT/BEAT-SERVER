package com.beat.contracts.schedule.readmodel;

import java.time.LocalDateTime;

import com.beat.contracts.common.ReadModel;

@ReadModel
public record MinPerformanceDate(
	Long performanceId,
	LocalDateTime performanceDate
) {
}
