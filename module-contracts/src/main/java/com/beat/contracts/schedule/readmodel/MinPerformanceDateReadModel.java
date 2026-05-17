package com.beat.contracts.schedule.readmodel;

import java.time.LocalDateTime;

import com.beat.contracts.common.ReadModel;

@ReadModel
public record MinPerformanceDateReadModel(
	Long performanceId,
	LocalDateTime performanceDate
) {
}
